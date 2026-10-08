package com.devrenno.bookland.orders.application.port.out;

import com.devrenno.bookland.orders.domain.entity.Order;

/**
 * Announces what happened to an order, to whoever cares. Unlike {@link CheckoutCommandPort} this asks
 * nothing of anyone and expects no answer: each consumer decides on its own what an event means to it
 * — the catalog and payments act on a cancellation, the notification service tells the customer.
 *
 * <p>Every event carries the order as it stands after the change — customer, email, items, total,
 * reason — so a consumer never has to ask orders, or anyone else, for more.
 *
 * <p>Implemented by writing to the orders outbox in the caller's transaction, so the event is
 * published if and only if the change it announces commits.
 */
public interface OrderEventPort {

    /** The payment was approved: the checkout succeeded. */
    void orderConfirmed(Order order);

    /** The payment was declined; the reserved stock is being given back. */
    void orderPaymentFailed(Order order);

    /** The stock could not be reserved; nothing was charged. */
    void orderRejected(Order order);

    void orderShipped(Order order);

    /** The order went from CONFIRMED to CANCELLED: its stock and its payment are owed back. */
    void orderCancelled(Order order);
}
