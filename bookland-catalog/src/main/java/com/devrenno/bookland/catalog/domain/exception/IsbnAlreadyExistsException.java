package com.devrenno.bookland.catalog.domain.exception;

public class IsbnAlreadyExistsException extends RuntimeException {

    public IsbnAlreadyExistsException(String isbn) {
        super("ISBN already registered: " + isbn);
    }
}
