package com.devrenno.bookland.catalog.domain.exception;

import java.util.UUID;

public class BookHasActiveOrdersException extends RuntimeException {

    public BookHasActiveOrdersException(UUID bookId) {
        super("Cannot remove book with active orders: " + bookId);
    }
}
