package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.port.in.GetCartUseCase;
import com.devrenno.bookland.orders.application.port.out.CartPersistencePort;
import com.devrenno.bookland.orders.domain.entity.Cart;

import java.util.UUID;

public class GetCartService implements GetCartUseCase {

    private final CartPersistencePort cartPersistencePort;

    private GetCartService(CartPersistencePort cartPersistencePort) {
        this.cartPersistencePort = cartPersistencePort;
    }

    public static GetCartService create(CartPersistencePort cartPersistencePort) {
        return new GetCartService(cartPersistencePort);
    }

    @Override
    public Cart execute(UUID customerId) {
        return cartPersistencePort.findByCustomerId(customerId)
                .orElseGet(() -> Cart.createFor(customerId));
    }
}
