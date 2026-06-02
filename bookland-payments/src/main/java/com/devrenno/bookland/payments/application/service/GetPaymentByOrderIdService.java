package com.devrenno.bookland.payments.application.service;

import com.devrenno.bookland.payments.application.annotation.UseCase;
import com.devrenno.bookland.payments.application.dto.PaymentResponse;
import com.devrenno.bookland.payments.application.port.in.GetPaymentByOrderIdUseCase;
import com.devrenno.bookland.payments.application.port.out.PaymentPersistencePort;
import com.devrenno.bookland.payments.domain.entity.Payment;
import com.devrenno.bookland.payments.domain.exception.PaymentNotFoundException;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class GetPaymentByOrderIdService implements GetPaymentByOrderIdUseCase {

    private final PaymentPersistencePort persistence;

    @Override
    public PaymentResponse getByOrderId(UUID orderId) {
        Payment payment = persistence.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(orderId));
        return toResponse(payment);
    }

    static PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getCustomerId(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getGatewayTransactionId(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
