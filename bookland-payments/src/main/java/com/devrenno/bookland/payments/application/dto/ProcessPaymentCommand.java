package com.devrenno.bookland.payments.application.dto;

import com.devrenno.bookland.payments.domain.entity.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

public record ProcessPaymentCommand(
        UUID orderId,
        UUID customerId,
        BigDecimal amount,
        PaymentMethod method
) {}
