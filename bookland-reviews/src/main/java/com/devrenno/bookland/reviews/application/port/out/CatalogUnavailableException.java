package com.devrenno.bookland.reviews.application.port.out;

/**
 * The catalog could not say whether the book exists — down, too slow, or its circuit breaker open.
 * Creating the review is refused (503) rather than done unchecked.
 */
public class CatalogUnavailableException extends RuntimeException {
    public CatalogUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
