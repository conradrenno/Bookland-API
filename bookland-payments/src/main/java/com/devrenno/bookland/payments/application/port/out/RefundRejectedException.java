package com.devrenno.bookland.payments.application.port.out;

/**
 * The gateway answered, and the answer is no — a refund window closed, a card account gone. Asking
 * again gets the same answer, so the refund is marked REFUND_FAILED for a person to settle.
 */
public class RefundRejectedException extends RuntimeException {
    public RefundRejectedException(String message) {
        super(message);
    }
}
