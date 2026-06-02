package com.devrenno.bookland.orders.application.port.in;

import com.devrenno.bookland.orders.application.dto.OrderResponse;

import java.util.UUID;

public interface CheckoutUseCase {
    OrderResponse execute(UUID customerId);
}
