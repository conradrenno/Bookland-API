package com.devrenno.bookland.orders.infrastructure.adapter;

import com.devrenno.bookland.orders.application.port.out.PaymentPort;
import com.devrenno.bookland.payments.application.dto.PaymentResult;
import com.devrenno.bookland.payments.application.dto.ProcessPaymentCommand;
import com.devrenno.bookland.payments.application.port.in.ProcessPaymentUseCase;
import com.devrenno.bookland.payments.domain.entity.PaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentAdapter implements PaymentPort {

    private final ProcessPaymentUseCase processPaymentUseCase;

    @Override
    public PaymentResult processPayment(UUID orderId, UUID customerId, BigDecimal amount, PaymentMethod method) {
        return processPaymentUseCase.processPayment(new ProcessPaymentCommand(orderId, customerId, amount, method));
    }
}
