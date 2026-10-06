package com.devrenno.bookland.orders.application.port.out;

import com.devrenno.bookland.orders.application.dto.StockLine;
import com.devrenno.bookland.orders.application.dto.StockReservationOutcome;

import java.util.List;
import java.util.UUID;

/**
 * Stock as the checkout saga needs it: taken per order, all lines or none, and given back per order.
 *
 * <p>Replaces the per-book decrement and increment this module used to call. Asking by order rather
 * than by book is what lets the catalog make both operations idempotent — the order id is the key it
 * records the reservation under — and what lets a cancellation return exactly what was taken without
 * this module having to remember it.
 */
public interface StockReservationPort {

    StockReservationOutcome reserve(UUID orderId, List<StockLine> lines);

    /** Gives back what the order reserved. Safe to call twice, or for an order that reserved nothing. */
    void release(UUID orderId);
}
