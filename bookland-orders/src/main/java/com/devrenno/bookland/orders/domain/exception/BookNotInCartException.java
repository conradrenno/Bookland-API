package com.devrenno.bookland.orders.domain.exception;

import java.util.UUID;

public class BookNotInCartException extends RuntimeException {
    public BookNotInCartException(UUID bookId) {
        super("Book not in cart: " + bookId);
    }
}
