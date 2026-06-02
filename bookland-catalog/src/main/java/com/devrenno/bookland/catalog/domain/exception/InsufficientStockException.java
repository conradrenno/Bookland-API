package com.devrenno.bookland.catalog.domain.exception;

import java.util.UUID;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(UUID bookId, int current, int delta) {
        super("Insufficient stock for book " + bookId + ": current=" + current + ", requested delta=" + delta);
    }
}
