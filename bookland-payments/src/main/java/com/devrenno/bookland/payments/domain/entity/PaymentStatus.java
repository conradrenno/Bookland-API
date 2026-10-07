package com.devrenno.bookland.payments.domain.entity;

/**
 * Where a payment stands. Two of the states mean "the gateway still owes us an answer" —
 * {@link #PENDING} for a charge, {@link #REFUND_PENDING} for a refund — and are what the gateway
 * worker picks up; the others are settled.
 */
public enum PaymentStatus {
    /** Charge requested, not yet answered by the gateway. */
    PENDING,
    APPROVED,
    DECLINED,
    /** Refund requested, not yet confirmed by the gateway. */
    REFUND_PENDING,
    REFUNDED,
    /** The gateway refused the refund for good: the money did not go back, and a person has to act. */
    REFUND_FAILED
}
