package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.annotation.UseCase;
import com.devrenno.bookland.orders.application.dto.AddCartItemCommand;
import com.devrenno.bookland.orders.application.dto.BookInfo;
import com.devrenno.bookland.orders.application.dto.CartResponse;
import com.devrenno.bookland.orders.application.port.in.AddCartItemUseCase;
import com.devrenno.bookland.orders.application.port.out.BookInfoPort;
import com.devrenno.bookland.orders.application.port.out.CartPersistencePort;
import com.devrenno.bookland.orders.domain.entity.Cart;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class AddCartItemService implements AddCartItemUseCase {

    private final CartPersistencePort cartPersistencePort;
    private final BookInfoPort bookInfoPort;

    @Override
    public CartResponse execute(AddCartItemCommand command) {
        BookInfo book = bookInfoPort.getBookInfo(command.bookId());
        Cart cart = cartPersistencePort.findByCustomerId(command.customerId())
                .orElseGet(() -> Cart.createFor(command.customerId()));
        cart.addOrUpdateItem(command.bookId(), book.price(), command.quantity(), book.stockQuantity());
        return OrderResponseMapper.toCartResponse(cartPersistencePort.save(cart));
    }
}
