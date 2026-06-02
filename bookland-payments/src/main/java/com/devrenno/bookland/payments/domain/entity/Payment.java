package com.devrenno.bookland.payments.domain.entity;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class Payment {

    private final UUID id;
    private final UUID orderId;
    private final UUID customerId;
    private final BigDecimal amount;
    private final PaymentMethod method;
    private PaymentStatus status;
    private final String gatewayTransactionId;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Payment create(UUID orderId, UUID customerId, BigDecimal amount,
                                 PaymentMethod method, PaymentStatus status, String gatewayTransactionId) {
        return Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .customerId(customerId)
                .amount(amount)
                .method(method)
                .status(status)
                .gatewayTransactionId(gatewayTransactionId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public void markRefunded() {
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = LocalDateTime.now();
    }
}
