package com.devrenno.bookland.payments.application.service;

import com.devrenno.bookland.payments.application.port.in.GetPaymentByOrderIdUseCase;
import com.devrenno.bookland.payments.application.port.out.PaymentPersistencePort;
import com.devrenno.bookland.payments.domain.entity.Payment;
import com.devrenno.bookland.payments.domain.exception.PaymentAccessDeniedException;
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

    /**
     * The owner is on the payment itself ({@code customerId}), so unlike the user aggregate this
     * one has to be loaded before the question can be answered — a missing payment is a 404 and
     * somebody else's is a 403, the same order the orders module already uses.
     */
    @Override
    public Payment getByOrderId(UUID orderId, UUID requesterId) {
        Payment payment = persistence.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(orderId));

        if (!payment.getCustomerId().equals(requesterId)) {
            throw new PaymentAccessDeniedException(orderId);
        }
        return payment;
    }
}
