package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.dto.CartView;
import com.devrenno.bookland.orders.application.port.in.GetCartUseCase;
import com.devrenno.bookland.orders.application.port.out.BookInfoPort;
import com.devrenno.bookland.orders.application.port.out.CartPersistencePort;
import com.devrenno.bookland.orders.domain.entity.Cart;

import java.util.UUID;

public class GetCartService implements GetCartUseCase {

    private final CartPersistencePort cartPersistencePort;
    private final BookInfoPort bookInfoPort;

    private GetCartService(CartPersistencePort cartPersistencePort, BookInfoPort bookInfoPort) {
        this.cartPersistencePort = cartPersistencePort;
        this.bookInfoPort = bookInfoPort;
    }

    public static GetCartService create(CartPersistencePort cartPersistencePort, BookInfoPort bookInfoPort) {
        return new GetCartService(cartPersistencePort, bookInfoPort);
    }

    @Override
    public CartView execute(UUID customerId) {
        Cart cart = cartPersistencePort.findByCustomerId(customerId)
                .orElseGet(() -> Cart.createFor(customerId));
        return CartViewAssembler.toView(cart, bookInfoPort);
    }
}
