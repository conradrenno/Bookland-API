package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.port.in.AdjustBookStockUseCase;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;
import com.devrenno.bookland.catalog.domain.exception.InsufficientStockException;

import java.util.UUID;

/**
 * Signed administrative adjustment, kept as its own use case because it is the only stock operation
 * whose caller does not know in advance which direction it moves.
 *
 * <p>It routes the delta to the matching relative UPDATE rather than loading the book, adjusting it
 * in memory and writing the absolute result back. The old shape read stock=5, computed 5+delta and
 * saved that number, so two admins correcting the same book at once kept whichever write landed
 * last and silently discarded the other. Routing by sign preserves the contract that shape had —
 * {@link InsufficientStockException} when the correction would go negative — while letting the store
 * evaluate the guard in the statement that writes. This is the only place that guard lives now:
 * {@code Book} deliberately exposes no method to move stock, because any such method would have to
 * compute the new value from a snapshot the store may already have moved past.</p>
 */
public class AdjustBookStockService implements AdjustBookStockUseCase {

    private final BookPersistencePort bookPersistencePort;

    private AdjustBookStockService(BookPersistencePort bookPersistencePort) {
        this.bookPersistencePort = bookPersistencePort;
    }

    public static AdjustBookStockService create(BookPersistencePort bookPersistencePort) {
        return new AdjustBookStockService(bookPersistencePort);
    }

    /**
     * Returns the resulting stock. The caller is responsible for running this inside a transaction
     * if it intends to derive anything from the returned value — the read-back is only consistent
     * with this adjustment while the row lock taken by the UPDATE is still held.
     */
    @Override
    public int adjustStock(UUID bookId, int delta) {
        if (delta > 0) {
            if (!bookPersistencePort.incrementStock(bookId, delta)) {
                throw new BookNotFoundException(bookId);
            }
        } else if (delta < 0) {
            if (!bookPersistencePort.tryDecrementStock(bookId, -delta)) {
                // Zero rows means either the book is gone or the guard refused. Reading it back
                // tells the two apart, so the caller gets the same error it did before.
                throw new InsufficientStockException(bookId, currentStock(bookId), delta);
            }
        }
        return currentStock(bookId);
    }

    private int currentStock(UUID bookId) {
        return bookPersistencePort.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId))
                .getStockQuantity();
    }
}
