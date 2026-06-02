package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.annotation.UseCase;
import com.devrenno.bookland.orders.application.dto.CartResponse;
import com.devrenno.bookland.orders.application.port.in.RemoveCartItemUseCase;
import com.devrenno.bookland.orders.application.port.out.CartPersistencePort;
import com.devrenno.bookland.orders.domain.entity.Cart;
import com.devrenno.bookland.orders.domain.exception.CartNotFoundException;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class RemoveCartItemService implements RemoveCartItemUseCase {

    private final CartPersistencePort cartPersistencePort;

    @Override
    public CartResponse execute(UUID customerId, UUID bookId) {
        Cart cart = cartPersistencePort.findByCustomerId(customerId)
                .orElseThrow(() -> new CartNotFoundException(customerId));
        cart.removeItem(bookId);
        return OrderResponseMapper.toCartResponse(cartPersistencePort.save(cart));
    }
}
