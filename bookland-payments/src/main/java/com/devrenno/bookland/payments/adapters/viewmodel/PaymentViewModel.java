package com.devrenno.bookland.payments.adapters.viewmodel;

import com.devrenno.bookland.payments.domain.entity.PaymentMethod;
import com.devrenno.bookland.payments.domain.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentViewModel(
        UUID id,
        UUID orderId,
        UUID customerId,
        BigDecimal amount,
        PaymentMethod method,
        PaymentStatus status,
        String gatewayTransactionId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
