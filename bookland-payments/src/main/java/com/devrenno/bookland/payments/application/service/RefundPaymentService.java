package com.devrenno.bookland.payments.application.service;

import com.devrenno.bookland.payments.application.port.in.RefundPaymentUseCase;
import com.devrenno.bookland.payments.application.port.out.PaymentGatewayPort;
import com.devrenno.bookland.payments.application.port.out.PaymentPersistencePort;
import com.devrenno.bookland.payments.domain.entity.Payment;
import com.devrenno.bookland.payments.domain.entity.PaymentStatus;
import com.devrenno.bookland.payments.domain.exception.PaymentNotFoundException;
import com.devrenno.bookland.payments.domain.exception.RefundNotAllowedException;

import java.util.UUID;

public class RefundPaymentService implements RefundPaymentUseCase {

    private final PaymentPersistencePort persistence;
    private final PaymentGatewayPort gateway;

    private RefundPaymentService(PaymentPersistencePort persistence, PaymentGatewayPort gateway) {
        this.persistence = persistence;
        this.gateway = gateway;
    }

    public static RefundPaymentService create(PaymentPersistencePort persistence, PaymentGatewayPort gateway) {
        return new RefundPaymentService(persistence, gateway);
    }

    /**
     * Idempotent by order: a payment already refunded is left alone, so a redelivered
     * {@code OrderCancelled} does not reach the gateway a second time. Anything else that is not
     * APPROVED (no payment, or a declined one) means the order was never paid, and refusing is
     * the honest answer.
     */
    @Override
    public void refund(UUID orderId) {
        Payment payment = persistence.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(orderId));

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return;
        }
        if (payment.getStatus() != PaymentStatus.APPROVED) {
            throw new RefundNotAllowedException(orderId);
        }

        gateway.refund(payment.getGatewayTransactionId());
        payment.markRefunded();
        persistence.save(payment);
    }
}
