package com.devrenno.bookland.payments.application.service;

import com.devrenno.bookland.payments.application.annotation.UseCase;
import com.devrenno.bookland.payments.application.dto.PaymentResult;
import com.devrenno.bookland.payments.application.dto.ProcessPaymentCommand;
import com.devrenno.bookland.payments.application.port.in.ProcessPaymentUseCase;
import com.devrenno.bookland.payments.application.port.out.PaymentGatewayPort;
import com.devrenno.bookland.payments.application.port.out.PaymentPersistencePort;
import com.devrenno.bookland.payments.domain.entity.Payment;
import com.devrenno.bookland.payments.domain.entity.PaymentStatus;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ProcessPaymentService implements ProcessPaymentUseCase {

    private final PaymentGatewayPort gateway;
    private final PaymentPersistencePort persistence;

    @Override
    public PaymentResult processPayment(ProcessPaymentCommand command) {
        PaymentResult result = gateway.charge(command);

        PaymentStatus status = result.approved() ? PaymentStatus.APPROVED : PaymentStatus.DECLINED;
        Payment payment = Payment.create(
                command.orderId(),
                command.customerId(),
                command.amount(),
                command.method(),
                status,
                result.transactionId()
        );
        persistence.save(payment);

        return result;
    }
}
