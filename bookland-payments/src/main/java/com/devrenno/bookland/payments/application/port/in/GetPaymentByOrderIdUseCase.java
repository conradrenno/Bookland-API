package com.devrenno.bookland.payments.application.port.in;

import com.devrenno.bookland.payments.application.dto.PaymentResponse;

import java.util.UUID;

public interface GetPaymentByOrderIdUseCase {
    PaymentResponse getByOrderId(UUID orderId);
}
