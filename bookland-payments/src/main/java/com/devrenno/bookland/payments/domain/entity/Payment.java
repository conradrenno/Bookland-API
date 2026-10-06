package com.devrenno.bookland.payments.domain.entity;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
public class Payment {

    private final UUID id;
    private final UUID orderId;
    private final UUID customerId;
    private final BigDecimal amount;
    private final PaymentMethod method;
    private PaymentStatus status;
    private final String gatewayTransactionId;
    /** Why the gateway said no; null when it said yes. Kept so a repeated request can be answered alike. */
    private final String declineReason;
    private final Instant createdAt;
    private Instant updatedAt;

    private Payment(UUID id, UUID orderId, UUID customerId, BigDecimal amount, PaymentMethod method,
                    PaymentStatus status, String gatewayTransactionId, String declineReason,
                    Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.gatewayTransactionId = gatewayTransactionId;
        this.declineReason = declineReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Payment create(UUID orderId, UUID customerId, BigDecimal amount,
                                 PaymentMethod method, PaymentStatus status, String gatewayTransactionId,
                                 String declineReason) {
        Instant now = Instant.now();
        return new Payment(UUID.randomUUID(), orderId, customerId, amount, method, status, gatewayTransactionId,
                declineReason, now, now);
    }

    public static Payment reconstitute(UUID id, UUID orderId, UUID customerId, BigDecimal amount, PaymentMethod method,
                                       PaymentStatus status, String gatewayTransactionId, String declineReason,
                                       Instant createdAt, Instant updatedAt) {
        return new Payment(id, orderId, customerId, amount, method, status, gatewayTransactionId, declineReason,
                createdAt, updatedAt);
    }

    public void markRefunded() {
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = Instant.now();
    }
}
