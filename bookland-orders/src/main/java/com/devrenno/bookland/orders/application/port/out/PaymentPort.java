package com.devrenno.bookland.orders.application.port.out;

import com.devrenno.bookland.orders.application.dto.PaymentOutcome;
import com.devrenno.bookland.orders.domain.entity.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Charges an order. Idempotent on the other side by order id: charging the same order twice is
 * answered from the first payment, which is what will let this request travel as a message that
 * may be delivered more than once.
 */
public interface PaymentPort {
    PaymentOutcome charge(UUID orderId, UUID customerId, BigDecimal amount, PaymentMethod method);
}
