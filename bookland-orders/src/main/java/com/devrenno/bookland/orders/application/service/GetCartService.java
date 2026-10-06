package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.dto.CartView;
import com.devrenno.bookland.orders.application.port.in.GetCartUseCase;
import com.devrenno.bookland.orders.application.port.out.BookInfoPort;
import com.devrenno.bookland.orders.application.port.out.CartPersistencePort;

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
        return cartPersistencePort.findByCustomerId(customerId)
                .map(cart -> CartViewAssembler.toView(cart, bookInfoPort))
                .orElseGet(() -> CartView.emptyFor(customerId));
    }
}
