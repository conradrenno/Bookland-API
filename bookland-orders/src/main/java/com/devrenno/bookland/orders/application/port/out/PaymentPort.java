package com.devrenno.bookland.orders.application.port.out;

import com.devrenno.bookland.payments.application.dto.PaymentResult;
import com.devrenno.bookland.payments.domain.entity.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentPort {
    PaymentResult processPayment(UUID orderId, UUID customerId, BigDecimal amount, PaymentMethod method);
}
