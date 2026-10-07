package com.devrenno.bookland.payments.application.service;

import com.devrenno.bookland.payments.application.port.in.RequestRefundUseCase;
import com.devrenno.bookland.payments.application.port.out.PaymentPersistencePort;
import com.devrenno.bookland.payments.domain.entity.Payment;
import com.devrenno.bookland.payments.domain.exception.PaymentNotFoundException;

import java.time.Clock;
import java.util.UUID;

public class RequestRefundService implements RequestRefundUseCase {

    private final PaymentPersistencePort persistence;
    private final Clock clock;

    private RequestRefundService(PaymentPersistencePort persistence, Clock clock) {
        this.persistence = persistence;
        this.clock = clock;
    }

    public static RequestRefundService create(PaymentPersistencePort persistence, Clock clock) {
        return new RequestRefundService(persistence, clock);
    }

    /**
     * Only an approved payment has money to give back. Anything else that is not already being
     * refunded means the order was never paid, and refusing ({@code RefundNotAllowedException},
     * {@code PaymentNotFoundException}) is the honest answer.
     */
    @Override
    public void requestRefund(UUID orderId) {
        Payment payment = persistence.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(orderId));
        if (payment.requestRefund(clock.instant())) {
            persistence.save(payment);
        }
    }
}
