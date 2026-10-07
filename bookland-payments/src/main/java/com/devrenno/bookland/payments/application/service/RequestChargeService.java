package com.devrenno.bookland.payments.application.service;

import com.devrenno.bookland.payments.application.dto.ProcessPaymentCommand;
import com.devrenno.bookland.payments.application.port.in.RequestChargeUseCase;
import com.devrenno.bookland.payments.application.port.out.PaymentPersistencePort;
import com.devrenno.bookland.payments.domain.entity.Payment;

import java.time.Clock;

public class RequestChargeService implements RequestChargeUseCase {

    private final PaymentPersistencePort persistence;
    private final Clock clock;

    private RequestChargeService(PaymentPersistencePort persistence, Clock clock) {
        this.persistence = persistence;
        this.clock = clock;
    }

    public static RequestChargeService create(PaymentPersistencePort persistence, Clock clock) {
        return new RequestChargeService(persistence, clock);
    }

    /**
     * One payment per order: a repeated charge command finds the payment already recorded and leaves
     * it alone. Pending or settled, its answer goes to the saga once, from the gateway worker.
     */
    @Override
    public void requestCharge(ProcessPaymentCommand command) {
        if (persistence.findByOrderId(command.orderId()).isPresent()) {
            return;
        }
        persistence.save(Payment.requestCharge(command.orderId(), command.customerId(), command.amount(),
                command.method(), clock.instant()));
    }
}
