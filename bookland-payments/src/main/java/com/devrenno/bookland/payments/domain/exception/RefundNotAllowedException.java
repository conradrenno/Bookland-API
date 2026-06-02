package com.devrenno.bookland.payments.domain.exception;

import java.util.UUID;

public class RefundNotAllowedException extends RuntimeException {
    public RefundNotAllowedException(UUID orderId) {
        super("Refund not allowed for order: " + orderId + " — payment is not in APPROVED status");
    }
}
