package com.devrenno.bookland.catalog.application.port.in;

import java.util.UUID;

/**
 * Cross-module boundary for consuming stock (orders' checkout). Separate from
 * {@link AdjustBookStockUseCase} because the semantics differ: an adjustment applies an admin's
 * absolute intent and fails loudly when it would go negative, while a decrement competes with other
 * decrements and reports "there was not enough left" as an ordinary, expected outcome.
 */
public interface DecrementBookStockUseCase {

    /**
     * Consumes {@code quantity} units atomically. Returns false — without changing stock — when the
     * book is inactive or has fewer units left, which under concurrency is the normal way to lose
     * the race for the last copies.
     */
    boolean tryDecrement(UUID bookId, int quantity);
}
