package com.devrenno.bookland.reviews.domain.exception;

import java.util.UUID;

/**
 * The catalog does not have the book being reviewed. The reviews module's own exception — until step 5
 * the service threw the catalog's, the one leak of another module's type into these inner layers.
 * Same message and {@code BOOK_NOT_FOUND} code as the catalog's.
 */
public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException(UUID bookId) {
        super("Book not found: " + bookId);
    }
}
