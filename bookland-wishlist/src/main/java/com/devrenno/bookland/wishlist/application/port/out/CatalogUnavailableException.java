package com.devrenno.bookland.wishlist.application.port.out;

/**
 * The catalog could not be asked about books — down, too slow, or its circuit breaker open. Adding to
 * the wishlist refuses (503); rendering the wishlist degrades every item to "Unavailable".
 */
public class CatalogUnavailableException extends RuntimeException {
    public CatalogUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
