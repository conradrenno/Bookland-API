package com.devrenno.bookland.catalog.application.port.out;

/**
 * Orders could not be asked whether a book has active orders — down, or too slow. Removing the book
 * is refused rather than done unchecked: a removal that should have been blocked cannot be undone by
 * the order it strands.
 */
public class OrdersUnavailableException extends RuntimeException {
    public OrdersUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
