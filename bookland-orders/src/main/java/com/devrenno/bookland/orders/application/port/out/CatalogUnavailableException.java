package com.devrenno.bookland.orders.application.port.out;

/**
 * The catalog could not be asked about books — down, too slow, or its circuit breaker open. Not the
 * same as "the book does not exist": the answer is unknown, so a flow that must know (adding to the
 * cart, checking out) refuses with 503, and one that can do without (rendering the cart) degrades.
 */
public class CatalogUnavailableException extends RuntimeException {
    public CatalogUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
