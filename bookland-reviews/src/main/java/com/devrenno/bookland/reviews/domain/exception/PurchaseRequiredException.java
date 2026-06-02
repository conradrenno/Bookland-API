package com.devrenno.bookland.reviews.domain.exception;

import java.util.UUID;

public class PurchaseRequiredException extends RuntimeException {
    public PurchaseRequiredException(UUID customerId, UUID bookId) {
        super("Customer " + customerId + " has not purchased book " + bookId);
    }
}
