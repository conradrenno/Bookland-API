package com.devrenno.bookland.orders.domain.exception;

import java.util.UUID;

/**
 * The catalog does not have the book (never listed, or removed). The orders module's own exception:
 * the catalog's one lives in another service and never reaches here. Same message and the same
 * {@code BOOK_NOT_FOUND} code, so a client cannot tell which module answered.
 */
public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException(UUID bookId) {
        super("Book not found: " + bookId);
    }
}
