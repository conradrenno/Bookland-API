package com.devrenno.bookland.orders.application.port.in;

import com.devrenno.bookland.orders.domain.entity.Cart;

import java.util.UUID;

public interface RemoveCartItemUseCase {
    Cart execute(UUID customerId, UUID bookId);
}
