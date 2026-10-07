package com.devrenno.bookland.payments.application.port.out;

import com.devrenno.bookland.payments.domain.entity.Payment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentPersistencePort {
    Payment save(Payment payment);
    Optional<Payment> findById(UUID id);
    Optional<Payment> findByOrderId(UUID orderId);
    /** Payments waiting on the gateway (PENDING, REFUND_PENDING) whose next attempt is due by {@code now}, oldest first. */
    List<Payment> findDue(Instant now, int limit);
}
