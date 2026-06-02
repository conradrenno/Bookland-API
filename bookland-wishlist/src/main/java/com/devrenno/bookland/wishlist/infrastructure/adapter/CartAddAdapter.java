package com.devrenno.bookland.wishlist.infrastructure.adapter;

import com.devrenno.bookland.orders.application.dto.AddCartItemCommand;
import com.devrenno.bookland.orders.application.port.in.AddCartItemUseCase;
import com.devrenno.bookland.wishlist.application.port.out.CartAddPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CartAddAdapter implements CartAddPort {

    private final AddCartItemUseCase addCartItemUseCase;

    @Override
    public void addToCart(UUID customerId, UUID bookId, int quantity) {
        addCartItemUseCase.execute(new AddCartItemCommand(customerId, bookId, quantity));
    }
}
