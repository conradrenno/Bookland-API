package com.devrenno.bookland.payments.application.service;

import com.devrenno.bookland.payments.application.annotation.UseCase;
import com.devrenno.bookland.payments.application.port.in.RefundPaymentUseCase;
import com.devrenno.bookland.payments.application.port.out.PaymentGatewayPort;
import com.devrenno.bookland.payments.application.port.out.PaymentPersistencePort;
import com.devrenno.bookland.payments.domain.entity.Payment;
import com.devrenno.bookland.payments.domain.entity.PaymentStatus;
import com.devrenno.bookland.payments.domain.exception.PaymentNotFoundException;
import com.devrenno.bookland.payments.domain.exception.RefundNotAllowedException;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class RefundPaymentService implements RefundPaymentUseCase {

    private final PaymentPersistencePort persistence;
    private final PaymentGatewayPort gateway;

    @Override
    public void refund(UUID orderId) {
        Payment payment = persistence.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(orderId));

        if (payment.getStatus() != PaymentStatus.APPROVED) {
            throw new RefundNotAllowedException(orderId);
        }

        gateway.refund(payment.getGatewayTransactionId());
        payment.markRefunded();
        persistence.save(payment);
    }
}
