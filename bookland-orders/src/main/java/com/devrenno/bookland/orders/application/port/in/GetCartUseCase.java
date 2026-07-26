package com.devrenno.bookland.orders.application.port.in;

import com.devrenno.bookland.orders.application.dto.CartView;

import java.util.UUID;

public interface GetCartUseCase {
    CartView execute(UUID customerId);
}
