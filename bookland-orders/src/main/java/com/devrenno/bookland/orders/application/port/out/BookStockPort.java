package com.devrenno.bookland.orders.application.port.out;

import java.util.UUID;

/**
 * Both operations are relative rather than signed adjustments: orders only ever consumes or returns
 * a known number of units, and stating that in the port keeps the arithmetic where it can be applied
 * atomically instead of computed against a stock reading that may already be stale.
 */
public interface BookStockPort {

    /**
     * Consumes stock at checkout. Returns false when the units are no longer there — the caller is
     * expected to treat that as a normal outcome, not an error condition, because it is how a lost
     * race for the last copies surfaces.
     */
    boolean tryDecrementStock(UUID bookId, int quantity);

    /** Returns stock to the catalog when an order is cancelled. */
    void incrementStock(UUID bookId, int quantity);
}
