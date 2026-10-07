package com.devrenno.bookland.payments.domain.entity;

import com.devrenno.bookland.payments.domain.exception.RefundNotAllowedException;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A payment is recorded <em>before</em> the gateway is asked anything: first the intention
 * ({@link PaymentStatus#PENDING}, {@link PaymentStatus#REFUND_PENDING}), committed on its own, and only
 * then the call. So a message that requested the charge or the refund can never be lost to a gateway
 * that is down — the intention stays in the table, with when to try again ({@link #nextAttemptAt}),
 * how many times it was tried and why the last try failed.
 */
@Getter
public class Payment {

    private final UUID id;
    private final UUID orderId;
    private final UUID customerId;
    private final BigDecimal amount;
    private final PaymentMethod method;
    private PaymentStatus status;
    private String gatewayTransactionId;
    /** Why the gateway said no; null when it said yes. */
    private String declineReason;
    /** Failed calls to the gateway for the operation now pending; reset when a new one is requested. */
    private int attempts;
    /** When the pending operation is due at the gateway; null once nothing is pending. */
    private Instant nextAttemptAt;
    /** Why the last call failed, or why the gateway refused the refund. */
    private String lastError;
    private final Instant createdAt;
    private Instant updatedAt;

    private Payment(UUID id, UUID orderId, UUID customerId, BigDecimal amount, PaymentMethod method,
                    PaymentStatus status, String gatewayTransactionId, String declineReason, int attempts,
                    Instant nextAttemptAt, String lastError, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.gatewayTransactionId = gatewayTransactionId;
        this.declineReason = declineReason;
        this.attempts = attempts;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = lastError;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** A charge to be made: PENDING, due at once. */
    public static Payment requestCharge(UUID orderId, UUID customerId, BigDecimal amount, PaymentMethod method,
                                        Instant now) {
        return new Payment(UUID.randomUUID(), orderId, customerId, amount, method, PaymentStatus.PENDING, null,
                null, 0, now, null, now, now);
    }

    public static Payment reconstitute(UUID id, UUID orderId, UUID customerId, BigDecimal amount, PaymentMethod method,
                                       PaymentStatus status, String gatewayTransactionId, String declineReason,
                                       int attempts, Instant nextAttemptAt, String lastError,
                                       Instant createdAt, Instant updatedAt) {
        return new Payment(id, orderId, customerId, amount, method, status, gatewayTransactionId, declineReason,
                attempts, nextAttemptAt, lastError, createdAt, updatedAt);
    }

    /** True while the gateway still owes an answer: a charge or a refund requested and not settled. */
    public boolean awaitsGateway() {
        return status == PaymentStatus.PENDING || status == PaymentStatus.REFUND_PENDING;
    }

    public void approve(String transactionId, Instant now) {
        requireStatus(PaymentStatus.PENDING);
        this.status = PaymentStatus.APPROVED;
        this.gatewayTransactionId = transactionId;
        settled(now);
    }

    public void decline(String reason, Instant now) {
        requireStatus(PaymentStatus.PENDING);
        this.status = PaymentStatus.DECLINED;
        this.declineReason = reason;
        settled(now);
    }

    /**
     * Asks for the money back. Only an approved payment has money to return; one whose refund was
     * already requested — pending, done or refused — answers false and changes nothing, so a repeated
     * request is harmless. A payment that never took money (pending charge, declined) is refused.
     */
    public boolean requestRefund(Instant now) {
        switch (status) {
            case APPROVED -> {
                this.status = PaymentStatus.REFUND_PENDING;
                this.attempts = 0;
                this.lastError = null;
                this.nextAttemptAt = now;
                this.updatedAt = now;
                return true;
            }
            case REFUND_PENDING, REFUNDED, REFUND_FAILED -> {
                return false;
            }
            default -> throw new RefundNotAllowedException(orderId);
        }
    }

    public void markRefunded(Instant now) {
        requireStatus(PaymentStatus.REFUND_PENDING);
        this.status = PaymentStatus.REFUNDED;
        settled(now);
    }

    /** The gateway said no to the refund, for good: retrying would get the same answer. */
    public void refundRejected(String reason, Instant now) {
        requireStatus(PaymentStatus.REFUND_PENDING);
        this.status = PaymentStatus.REFUND_FAILED;
        this.lastError = reason;
        this.nextAttemptAt = null;
        this.updatedAt = now;
    }

    /**
     * The call did not get an answer (gateway down, timeout). The operation stays pending and is tried
     * again at {@code retryAt} — safe, because every call carries the same idempotency key, so a call
     * that did reach the gateway is not repeated by it.
     */
    public void gatewayCallFailed(String error, Instant retryAt, Instant now) {
        if (!awaitsGateway()) {
            throw new IllegalStateException("Payment " + id + " is " + status + ", nothing is pending at the gateway");
        }
        this.attempts++;
        this.lastError = error;
        this.nextAttemptAt = retryAt;
        this.updatedAt = now;
    }

    private void settled(Instant now) {
        this.nextAttemptAt = null;
        this.lastError = null;
        this.updatedAt = now;
    }

    private void requireStatus(PaymentStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Payment " + id + " is " + status + ", expected " + expected);
        }
    }
}
