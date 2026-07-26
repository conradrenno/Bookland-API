package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.dto.CartView;
import com.devrenno.bookland.orders.application.port.in.RemoveCartItemUseCase;
import com.devrenno.bookland.orders.application.port.out.BookInfoPort;
import com.devrenno.bookland.orders.application.port.out.CartPersistencePort;
import com.devrenno.bookland.orders.domain.entity.Cart;
import com.devrenno.bookland.orders.domain.exception.CartNotFoundException;

import java.util.UUID;

public class RemoveCartItemService implements RemoveCartItemUseCase {

    private final CartPersistencePort cartPersistencePort;
    private final BookInfoPort bookInfoPort;

    private RemoveCartItemService(CartPersistencePort cartPersistencePort, BookInfoPort bookInfoPort) {
        this.cartPersistencePort = cartPersistencePort;
        this.bookInfoPort = bookInfoPort;
    }

    public static RemoveCartItemService create(CartPersistencePort cartPersistencePort,
                                               BookInfoPort bookInfoPort) {
        return new RemoveCartItemService(cartPersistencePort, bookInfoPort);
    }

    @Override
    public CartView execute(UUID customerId, UUID bookId) {
        Cart cart = cartPersistencePort.findByCustomerId(customerId)
                .orElseThrow(() -> new CartNotFoundException(customerId));
        cart.removeItem(bookId);
        return CartViewAssembler.toView(cartPersistencePort.save(cart), bookInfoPort);
    }
}
