package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.annotation.UseCase;
import com.devrenno.bookland.orders.application.dto.CartResponse;
import com.devrenno.bookland.orders.application.port.in.GetCartUseCase;
import com.devrenno.bookland.orders.application.port.out.CartPersistencePort;
import com.devrenno.bookland.orders.domain.entity.Cart;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class GetCartService implements GetCartUseCase {

    private final CartPersistencePort cartPersistencePort;

    @Override
    public CartResponse execute(UUID customerId) {
        Cart cart = cartPersistencePort.findByCustomerId(customerId)
                .orElseGet(() -> Cart.createFor(customerId));
        return OrderResponseMapper.toCartResponse(cart);
    }
}
