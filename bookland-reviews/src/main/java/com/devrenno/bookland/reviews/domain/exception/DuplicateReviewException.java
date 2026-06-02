package com.devrenno.bookland.reviews.domain.exception;

import java.util.UUID;

public class DuplicateReviewException extends RuntimeException {
    public DuplicateReviewException(UUID customerId, UUID bookId) {
        super("Customer " + customerId + " has already reviewed book " + bookId);
    }
}
