package com.devrenno.bookland.catalog.application.port.in;

import java.util.UUID;

/**
 * Cross-module boundary for returning stock (orders' cancellation). The counterpart of
 * {@link DecrementBookStockUseCase}: there is no outcome to report because an increment has no
 * guard to fail, so a book that cannot be found is an error rather than an expected answer.
 */
public interface IncrementBookStockUseCase {

    /** Returns {@code quantity} units to the book, including a book delisted from the catalog. */
    void increment(UUID bookId, int quantity);
}
