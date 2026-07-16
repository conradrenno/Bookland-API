package com.devrenno.bookland.orders.application.port.in;

import com.devrenno.bookland.orders.domain.entity.Order;

import java.util.UUID;

public interface CancelOrderUseCase {
    Order execute(UUID orderId, UUID customerId);
}
