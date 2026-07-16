package com.devrenno.bookland.payments.application.service;

import com.devrenno.bookland.payments.application.port.in.GetPaymentByOrderIdUseCase;
import com.devrenno.bookland.payments.application.port.out.PaymentPersistencePort;
import com.devrenno.bookland.payments.domain.entity.Payment;
import com.devrenno.bookland.payments.domain.exception.PaymentNotFoundException;

import java.util.UUID;

public class GetPaymentByOrderIdService implements GetPaymentByOrderIdUseCase {

    private final PaymentPersistencePort persistence;

    private GetPaymentByOrderIdService(PaymentPersistencePort persistence) {
        this.persistence = persistence;
    }

    public static GetPaymentByOrderIdService create(PaymentPersistencePort persistence) {
        return new GetPaymentByOrderIdService(persistence);
    }

    @Override
    public Payment getByOrderId(UUID orderId) {
        return persistence.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(orderId));
    }
}
