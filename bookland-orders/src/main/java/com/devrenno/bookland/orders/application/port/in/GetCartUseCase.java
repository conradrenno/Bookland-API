package com.devrenno.bookland.orders.application.port.in;

import com.devrenno.bookland.orders.application.dto.CartResponse;

import java.util.UUID;

public interface GetCartUseCase {
    CartResponse execute(UUID customerId);
}
