package com.devrenno.bookland.orders.application.port.out;

import java.util.UUID;

/**
 * Announces what happened to an order, to whoever cares. Unlike {@link CheckoutCommandPort} this asks
 * nothing of anyone and expects no answer: the catalog and payments decide on their own what an event
 * means to them.
 *
 * <p>Implemented by writing to the orders outbox in the caller's transaction, so the event is
 * published if and only if the change it announces commits.
 */
public interface OrderEventPort {

    /** The order went from CONFIRMED to CANCELLED: its stock and its payment are owed back. */
    void orderCancelled(UUID orderId);
}
