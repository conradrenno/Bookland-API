package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.annotation.UseCase;
import com.devrenno.bookland.orders.application.dto.BookInfo;
import com.devrenno.bookland.orders.application.dto.CartResponse;
import com.devrenno.bookland.orders.application.dto.UpdateCartItemCommand;
import com.devrenno.bookland.orders.application.port.in.UpdateCartItemUseCase;
import com.devrenno.bookland.orders.application.port.out.BookInfoPort;
import com.devrenno.bookland.orders.application.port.out.CartPersistencePort;
import com.devrenno.bookland.orders.domain.entity.Cart;
import com.devrenno.bookland.orders.domain.exception.CartNotFoundException;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdateCartItemService implements UpdateCartItemUseCase {

    private final CartPersistencePort cartPersistencePort;
    private final BookInfoPort bookInfoPort;

    @Override
    public CartResponse execute(UpdateCartItemCommand command) {
        Cart cart = cartPersistencePort.findByCustomerId(command.customerId())
                .orElseThrow(() -> new CartNotFoundException(command.customerId()));
        BookInfo book = bookInfoPort.getBookInfo(command.bookId());
        cart.updateItemQuantity(command.bookId(), command.quantity(), book.stockQuantity());
        return OrderResponseMapper.toCartResponse(cartPersistencePort.save(cart));
    }
}
