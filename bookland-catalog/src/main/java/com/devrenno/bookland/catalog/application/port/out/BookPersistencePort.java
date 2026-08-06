package com.devrenno.bookland.catalog.application.port.out;

import com.devrenno.bookland.catalog.application.common.PageQuery;
import com.devrenno.bookland.catalog.application.common.PageResult;
import com.devrenno.bookland.catalog.application.dto.BookSearchQuery;
import com.devrenno.bookland.catalog.domain.entity.Book;

import java.util.Optional;
import java.util.UUID;

public interface BookPersistencePort {
    Book save(Book book);

    /**
     * Atomically decrements stock <em>for a sale</em>, refusing to go negative. Returns false when
     * the book is no longer active or no longer has {@code quantity} units, leaving stock untouched.
     *
     * <p>These relative operations exist next to {@link #save(Book)} because the read-modify-write
     * cycle behind {@code save} cannot enforce the "stock never goes negative" invariant under
     * concurrency: two checkouts both read stock=1, both compute 0, and both write the absolute
     * value 0 — one unit oversold, with no error raised. The guard has to be evaluated by the store,
     * in the same statement that writes, which is why this is a port operation rather than a domain
     * method.</p>
     */
    boolean tryDecrementSellableStock(UUID bookId, int quantity);

    /**
     * Atomically decrements stock <em>for an administrative correction</em>. Same guard against
     * going negative, but no {@code active} filter: admin write flows deliberately still see books
     * that were delisted from the catalog, and refusing to correct their count would strand it.
     */
    boolean tryDecrementStock(UUID bookId, int quantity);

    /**
     * Atomically returns units to a book's stock, delisted books included. Reports whether the book
     * was found; there is no guard to fail, since an increment cannot violate the invariant.
     *
     * <p>Relative for the same reason as the decrements: two cancellations reading the same stock
     * and writing back absolute values lose one of the two increments. That error is quieter than an
     * oversell — stock ends up understated, so the shop refuses sales it could have made — but it is
     * the same lost update and it corrupts the count just as permanently.</p>
     */
    boolean incrementStock(UUID bookId, int quantity);

    Optional<Book> findById(UUID id);
    Optional<Book> findByIsbn(String isbn);
    boolean existsByIsbn(String isbn);
    PageResult<Book> search(BookSearchQuery query);
    PageResult<Book> findByCategoryId(UUID categoryId, PageQuery pageQuery);
    PageResult<Book> findLowStock(int threshold, PageQuery pageQuery);
}
