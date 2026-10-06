package com.devrenno.bookland.payments.application.service;

import com.devrenno.bookland.payments.application.dto.PaymentResult;
import com.devrenno.bookland.payments.application.dto.ProcessPaymentCommand;
import com.devrenno.bookland.payments.application.port.in.ProcessPaymentUseCase;
import com.devrenno.bookland.payments.application.port.out.PaymentGatewayPort;
import com.devrenno.bookland.payments.application.port.out.PaymentPersistencePort;
import com.devrenno.bookland.payments.domain.entity.Payment;
import com.devrenno.bookland.payments.domain.entity.PaymentStatus;

import java.util.Optional;

public class ProcessPaymentService implements ProcessPaymentUseCase {

    private final PaymentGatewayPort gateway;
    private final PaymentPersistencePort persistence;

    private ProcessPaymentService(PaymentGatewayPort gateway, PaymentPersistencePort persistence) {
        this.gateway = gateway;
        this.persistence = persistence;
    }

    public static ProcessPaymentService create(PaymentGatewayPort gateway, PaymentPersistencePort persistence) {
        return new ProcessPaymentService(gateway, persistence);
    }

    /**
     * One payment per order. A request for an order that already has one — a redelivered message, a
     * retried call — is answered from the stored payment, without charging again: the gateway is the
     * one step of the checkout that cannot be taken back by writing to our own database.
     */
    @Override
    public PaymentResult processPayment(ProcessPaymentCommand command) {
        Optional<Payment> existing = persistence.findByOrderId(command.orderId());
        if (existing.isPresent()) {
            Payment payment = existing.get();
            return new PaymentResult(payment.getStatus() != PaymentStatus.DECLINED,
                    payment.getGatewayTransactionId(), payment.getDeclineReason());
        }

        PaymentResult result = gateway.charge(command);

        PaymentStatus status = result.approved() ? PaymentStatus.APPROVED : PaymentStatus.DECLINED;
        Payment payment = Payment.create(
                command.orderId(),
                command.customerId(),
                command.amount(),
                command.method(),
                status,
                result.transactionId(),
                result.declineReason()
        );
        persistence.save(payment);

        return result;
    }
}
