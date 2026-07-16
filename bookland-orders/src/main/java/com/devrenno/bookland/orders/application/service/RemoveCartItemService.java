package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.port.in.RemoveCartItemUseCase;
import com.devrenno.bookland.orders.application.port.out.CartPersistencePort;
import com.devrenno.bookland.orders.domain.entity.Cart;
import com.devrenno.bookland.orders.domain.exception.CartNotFoundException;

import java.util.UUID;

public class RemoveCartItemService implements RemoveCartItemUseCase {

    private final CartPersistencePort cartPersistencePort;

    private RemoveCartItemService(CartPersistencePort cartPersistencePort) {
        this.cartPersistencePort = cartPersistencePort;
    }

    public static RemoveCartItemService create(CartPersistencePort cartPersistencePort) {
        return new RemoveCartItemService(cartPersistencePort);
    }

    @Override
    public Cart execute(UUID customerId, UUID bookId) {
        Cart cart = cartPersistencePort.findByCustomerId(customerId)
                .orElseThrow(() -> new CartNotFoundException(customerId));
        cart.removeItem(bookId);
        return cartPersistencePort.save(cart);
    }
}
