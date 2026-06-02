package com.devrenno.bookland.orders.infrastructure.adapter;

import com.devrenno.bookland.orders.application.port.out.RefundPort;
import com.devrenno.bookland.payments.application.port.in.RefundPaymentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RefundAdapter implements RefundPort {

    private final RefundPaymentUseCase refundPaymentUseCase;

    @Override
    public void refund(UUID orderId) {
        refundPaymentUseCase.refund(orderId);
    }
}
