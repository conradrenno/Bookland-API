package com.devrenno.bookland.orders.application.port.in;

import com.devrenno.bookland.orders.application.dto.OrderResponse;

import java.util.UUID;

public interface CancelOrderUseCase {
    OrderResponse execute(UUID orderId, UUID customerId);
}
