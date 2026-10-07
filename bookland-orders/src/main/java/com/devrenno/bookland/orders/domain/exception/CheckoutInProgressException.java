package com.devrenno.bookland.orders.domain.exception;

import java.util.UUID;

/** A checkout for this customer has not finished yet; a second one would compete for the same cart. */
public class CheckoutInProgressException extends RuntimeException {
    public CheckoutInProgressException(UUID customerId) {
        super("A checkout is already in progress for customer " + customerId);
    }
}
