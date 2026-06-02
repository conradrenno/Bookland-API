package com.devrenno.bookland.orders.application.port.in;

import com.devrenno.bookland.orders.application.dto.OrderResponse;

import java.util.UUID;

public interface GetOrderByIdUseCase {
    OrderResponse execute(UUID orderId, UUID requesterId, boolean isAdmin);
}
