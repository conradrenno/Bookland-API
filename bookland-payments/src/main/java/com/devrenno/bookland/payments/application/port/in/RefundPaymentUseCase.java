package com.devrenno.bookland.payments.application.port.in;

import java.util.UUID;

public interface RefundPaymentUseCase {
    void refund(UUID orderId);
}
