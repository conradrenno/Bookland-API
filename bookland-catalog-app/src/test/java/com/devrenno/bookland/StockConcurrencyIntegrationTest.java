package com.devrenno.bookland;

import com.devrenno.bookland.catalog.application.port.in.AdjustBookStockUseCase;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.domain.entity.Book;
import com.devrenno.bookland.catalog.domain.exception.InsufficientStockException;
import com.devrenno.bookland.catalog.domain.valueobject.CategoryId;
import com.devrenno.bookland.catalog.domain.valueobject.ISBN;
import com.devrenno.bookland.catalog.domain.valueobject.Price;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Stock survives concurrency in both directions: never oversold however many checkouts race for the
 * last copies, and never short-changed however many cancellations return units at once.
 *
 * <p>This runs against the real UPDATEs and the real database because the invariants it pins cannot
 * be observed anywhere else. The previous implementation read the book, adjusted in Java and wrote
 * the absolute result back, so two transactions both read stock=1, both computed 0, and both wrote
 * 0 — one unit sold twice, no exception raised, nothing a mocked port could have caught. A test
 * with a stubbed persistence port would have passed against the broken code.</p>
 */
@CatalogIntegrationTest
class StockConcurrencyIntegrationTest {

    /** Seeded by V2 and referenced literally by DevDataLoader; any active category works here. */
    private static final UUID CAT_TECNOLOGIA = UUID.fromString("c3d4e5f6-a7b8-9012-cdef-123456789012");

    @Autowired
    private BookPersistencePort books;

    @Autowired
    private AdjustBookStockUseCase adjustBookStock;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("a decrement larger than the stock is refused, leaving the stock untouched")
    void refusesToGoNegative() {
        UUID bookId = persistBook(3);

        boolean applied = decrement(bookId, 5);

        assertThat(applied).isFalse();
        assertThat(stockOf(bookId)).isEqualTo(3);
    }

    @Test
    @DisplayName("stock can be drawn down to exactly zero, and not one unit further")
    void decrementsToZeroThenRefuses() {
        UUID bookId = persistBook(2);

        assertThat(decrement(bookId, 2)).isTrue();
        assertThat(stockOf(bookId)).isZero();

        assertThat(decrement(bookId, 1)).isFalse();
        assertThat(stockOf(bookId)).isZero();
    }

    @Test
    @DisplayName("a book removed from the catalog cannot have stock drawn from it")
    void refusesForInactiveBook() {
        UUID bookId = persistBook(5);
        deactivate(bookId);

        assertThat(decrement(bookId, 1)).isFalse();
        assertThat(stockOf(bookId)).isEqualTo(5);
    }

    /**
     * The asymmetry between the two guards: a delisted book must not be sold, but units returned by
     * a cancelled order still belong on the shelf. Dropping them would leave the count permanently
     * wrong if the book is ever relisted.
     */
    @Test
    @DisplayName("a book removed from the catalog still accepts stock coming back")
    void acceptsIncrementForInactiveBook() {
        UUID bookId = persistBook(5);
        deactivate(bookId);

        assertThat(increment(bookId, 2)).isTrue();
        assertThat(stockOf(bookId)).isEqualTo(7);
    }

    @Test
    @DisplayName("an increment for a book that does not exist matches no row")
    void incrementReportsMissingBook() {
        assertThat(increment(UUID.randomUUID(), 1)).isFalse();
    }

    /**
     * Twenty threads compete for five copies, released together so the reads genuinely overlap.
     *
     * <p>Safety is asserted unconditionally: units granted never exceed units held, and the row
     * accounts for exactly what was granted. Liveness — that all five copies actually sell — is
     * asserted only when no contender hit a database-level refusal, because the two engines lose
     * the race differently. PostgreSQL blocks the second writer on the row lock and re-evaluates
     * the guard, answering false; H2's MVStore may instead reject the concurrent update outright
     * and fail the transaction. Both refuse to oversell, which is the property under test, and the
     * checkout treats a false and a rolled-back transaction the same way. Against H2 in practice
     * the refusal path does not trigger and the strict count runs.</p>
     */
    @Test
    @DisplayName("concurrent decrements never hand out more units than exist")
    void concurrentDecrementsNeverOversell() throws Exception {
        int initialStock = 5;
        int contenders = 20;
        UUID bookId = persistBook(initialStock);

        CountDownLatch startLine = new CountDownLatch(1);
        AtomicInteger granted = new AtomicInteger();
        AtomicInteger refusedByDatabase = new AtomicInteger();

        try (ExecutorService pool = Executors.newFixedThreadPool(contenders)) {
            List<Callable<Void>> attempts = new ArrayList<>();
            for (int i = 0; i < contenders; i++) {
                attempts.add(() -> {
                    startLine.await();
                    try {
                        if (decrement(bookId, 1)) {
                            granted.incrementAndGet();
                        }
                    } catch (RuntimeException concurrentUpdateRejected) {
                        // The transaction rolled back, so no unit left the shelf either way.
                        refusedByDatabase.incrementAndGet();
                    }
                    return null;
                });
            }
            List<Future<Void>> running = attempts.stream().map(pool::submit).toList();
            startLine.countDown();
            for (Future<Void> f : running) {
                f.get();
            }
        }

        int remaining = stockOf(bookId);

        assertThat(granted.get())
                .as("units granted must never exceed units held")
                .isBetween(1, initialStock);
        assertThat(remaining)
                .as("stock must account for exactly the granted units, and never go negative")
                .isEqualTo(initialStock - granted.get())
                .isNotNegative();

        if (refusedByDatabase.get() == 0) {
            assertThat(granted.get())
                    .as("with every contender answered cleanly, the shelf should empty")
                    .isEqualTo(initialStock);
        }
    }

    /**
     * Twenty cancellations return a unit each to a book that started empty. A lost update here is
     * quieter than an oversell — the shop simply refuses sales it could have made — but it corrupts
     * the count just as permanently, so every increment has to land.
     */
    @Test
    @DisplayName("concurrent increments never lose a returned unit")
    void concurrentIncrementsNeverLoseAUnit() throws Exception {
        int returns = 20;
        UUID bookId = persistBook(0);

        CountDownLatch startLine = new CountDownLatch(1);
        AtomicInteger applied = new AtomicInteger();
        AtomicInteger refusedByDatabase = new AtomicInteger();

        try (ExecutorService pool = Executors.newFixedThreadPool(returns)) {
            List<Callable<Void>> attempts = new ArrayList<>();
            for (int i = 0; i < returns; i++) {
                attempts.add(() -> {
                    startLine.await();
                    try {
                        if (increment(bookId, 1)) {
                            applied.incrementAndGet();
                        }
                    } catch (RuntimeException concurrentUpdateRejected) {
                        refusedByDatabase.incrementAndGet();
                    }
                    return null;
                });
            }
            List<Future<Void>> running = attempts.stream().map(pool::submit).toList();
            startLine.countDown();
            for (Future<Void> f : running) {
                f.get();
            }
        }

        assertThat(stockOf(bookId))
                .as("stock must account for every increment that reported success")
                .isEqualTo(applied.get());

        if (refusedByDatabase.get() == 0) {
            assertThat(applied.get())
                    .as("with every caller answered cleanly, no returned unit may go missing")
                    .isEqualTo(returns);
        }
    }

    /**
     * The admin correction path, which routes a signed delta to the same relative UPDATEs. Twenty
     * corrections of +1 each must all land: this is the path that used to load the book, add the
     * delta in Java and write the absolute result back, keeping whichever write committed last.
     */
    @Test
    @DisplayName("concurrent admin adjustments never lose a correction")
    void concurrentAdjustmentsNeverLoseACorrection() throws Exception {
        int corrections = 20;
        UUID bookId = persistBook(0);

        CountDownLatch startLine = new CountDownLatch(1);
        AtomicInteger applied = new AtomicInteger();
        AtomicInteger refusedByDatabase = new AtomicInteger();

        try (ExecutorService pool = Executors.newFixedThreadPool(corrections)) {
            List<Callable<Void>> attempts = new ArrayList<>();
            for (int i = 0; i < corrections; i++) {
                attempts.add(() -> {
                    startLine.await();
                    try {
                        transactionTemplate.execute(tx -> adjustBookStock.adjustStock(bookId, 1));
                        applied.incrementAndGet();
                    } catch (RuntimeException concurrentUpdateRejected) {
                        refusedByDatabase.incrementAndGet();
                    }
                    return null;
                });
            }
            List<Future<Void>> running = attempts.stream().map(pool::submit).toList();
            startLine.countDown();
            for (Future<Void> f : running) {
                f.get();
            }
        }

        assertThat(stockOf(bookId))
                .as("stock must account for every adjustment that reported success")
                .isEqualTo(applied.get());

        if (refusedByDatabase.get() == 0) {
            assertThat(applied.get())
                    .as("with every caller answered cleanly, no correction may go missing")
                    .isEqualTo(corrections);
        }
    }

    /** The admin path reaches delisted books, unlike the sellable decrement the checkout uses. */
    @Test
    @DisplayName("an admin adjustment still corrects a book removed from the catalog")
    void adjustmentReachesInactiveBook() {
        UUID bookId = persistBook(4);
        deactivate(bookId);

        int result = transactionTemplate.execute(tx -> adjustBookStock.adjustStock(bookId, -3));

        assertThat(result).isEqualTo(1);
        assertThat(stockOf(bookId)).isEqualTo(1);
    }

    @Test
    @DisplayName("an admin adjustment below zero is refused with InsufficientStock")
    void adjustmentBelowZeroIsRefused() {
        UUID bookId = persistBook(2);

        assertThatThrownBy(() -> transactionTemplate.execute(tx -> adjustBookStock.adjustStock(bookId, -3)))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(stockOf(bookId)).isEqualTo(2);
    }

    private boolean decrement(UUID bookId, int quantity) {
        return Boolean.TRUE.equals(
                transactionTemplate.execute(tx -> books.tryDecrementSellableStock(bookId, quantity)));
    }

    private boolean increment(UUID bookId, int quantity) {
        return Boolean.TRUE.equals(
                transactionTemplate.execute(tx -> books.incrementStock(bookId, quantity)));
    }

    private void deactivate(UUID bookId) {
        transactionTemplate.execute(tx -> {
            Book book = books.findById(bookId).orElseThrow();
            book.deactivate();
            return books.save(book);
        });
    }

    private int stockOf(UUID bookId) {
        return transactionTemplate.execute(tx -> books.findById(bookId).orElseThrow().getStockQuantity());
    }

    private UUID persistBook(int stockQuantity) {
        Book book = Book.create(
                "Concurrency Fixture", ISBN.of(randomIsbn()), List.of("Author"), "Publisher",
                2026, "1st", null, Price.of(BigDecimal.TEN), stockQuantity,
                CategoryId.of(CAT_TECNOLOGIA), null);
        return transactionTemplate.execute(tx -> books.save(book)).getId().value();
    }

    /** The isbn column is unique, and this class writes several books per run. */
    private static String randomIsbn() {
        StringBuilder isbn = new StringBuilder(13);
        for (int i = 0; i < 13; i++) {
            isbn.append(ThreadLocalRandom.current().nextInt(10));
        }
        return isbn.toString();
    }
}
