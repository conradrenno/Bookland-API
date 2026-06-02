package com.devrenno.bookland.payments.application.port.out;

import com.devrenno.bookland.payments.domain.entity.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentPersistencePort {
    Payment save(Payment payment);
    Optional<Payment> findByOrderId(UUID orderId);
}
