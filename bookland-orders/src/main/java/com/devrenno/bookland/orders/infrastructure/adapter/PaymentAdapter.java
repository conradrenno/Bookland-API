package com.devrenno.bookland.orders.infrastructure.adapter;

import com.devrenno.bookland.orders.application.dto.PaymentOutcome;
import com.devrenno.bookland.orders.application.port.out.PaymentPort;
import com.devrenno.bookland.orders.domain.entity.PaymentMethod;
import com.devrenno.bookland.payments.application.dto.PaymentResult;
import com.devrenno.bookland.payments.application.dto.ProcessPaymentCommand;
import com.devrenno.bookland.payments.application.port.in.ProcessPaymentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The one place the orders module meets the payments module's types: its {@code PaymentMethod} and
 * {@code PaymentResult} are translated here and go no further in.
 */
@Component
@RequiredArgsConstructor
public class PaymentAdapter implements PaymentPort {

    private final ProcessPaymentUseCase processPaymentUseCase;

    @Override
    public PaymentOutcome charge(UUID orderId, UUID customerId, BigDecimal amount, PaymentMethod method) {
        PaymentResult result = processPaymentUseCase.processPayment(new ProcessPaymentCommand(
                orderId, customerId, amount,
                com.devrenno.bookland.payments.domain.entity.PaymentMethod.valueOf(method.name())));
        return new PaymentOutcome(result.approved(), result.declineReason());
    }
}
