package com.devrenno.bookland.orders.application.port.in;

import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.payments.domain.entity.PaymentMethod;

import java.util.UUID;

public interface CheckoutUseCase {
    Order execute(UUID customerId, PaymentMethod paymentMethod);
}
