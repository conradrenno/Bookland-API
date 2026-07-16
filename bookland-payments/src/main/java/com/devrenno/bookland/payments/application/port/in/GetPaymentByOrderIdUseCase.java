package com.devrenno.bookland.payments.application.port.in;

import com.devrenno.bookland.payments.domain.entity.Payment;

import java.util.UUID;

public interface GetPaymentByOrderIdUseCase {
    Payment getByOrderId(UUID orderId);
}
