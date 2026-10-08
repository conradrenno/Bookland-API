package com.devrenno.bookland.orders.domain.exception;

import java.util.UUID;

/**
 * Checkout with nothing to buy. A conflict with the cart's state (409), not a missing resource: to
 * the customer an absent cart and an empty one are the same thing — {@code GET /cart} shows both as
 * an empty cart.
 */
public class CartEmptyException extends RuntimeException {
    public CartEmptyException(UUID customerId) {
        super("The cart of customer " + customerId + " is empty");
    }
}
