package com.devrenno.bookland.wishlist.domain.exception;

import java.util.UUID;

/**
 * The catalog does not have the book. The wishlist's own exception — the catalog's lives in another
 * service — with the same message and {@code BOOK_NOT_FOUND} code, so a client cannot tell who answered.
 */
public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException(UUID bookId) {
        super("Book not found: " + bookId);
    }
}
