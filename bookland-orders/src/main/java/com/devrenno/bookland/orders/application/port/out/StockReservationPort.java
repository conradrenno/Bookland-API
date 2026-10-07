package com.devrenno.bookland.orders.application.port.out;

import java.util.UUID;

/**
 * Gives an order's reserved stock back — what a cancellation owes. Reserving moved to the checkout
 * saga ({@link CheckoutCommandPort}); releasing on cancellation follows in the next block, as an event.
 */
public interface StockReservationPort {

    /** Gives back what the order reserved. Safe to call twice, or for an order that reserved nothing. */
    void release(UUID orderId);
}
