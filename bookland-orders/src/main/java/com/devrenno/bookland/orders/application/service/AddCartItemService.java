package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.dto.AddCartItemCommand;
import com.devrenno.bookland.orders.application.dto.BookInfo;
import com.devrenno.bookland.orders.application.dto.CartView;
import com.devrenno.bookland.orders.application.port.in.AddCartItemUseCase;
import com.devrenno.bookland.orders.application.port.out.BookInfoPort;
import com.devrenno.bookland.orders.application.port.out.CartPersistencePort;
import com.devrenno.bookland.orders.domain.entity.Cart;

public class AddCartItemService implements AddCartItemUseCase {

    private final CartPersistencePort cartPersistencePort;
    private final BookInfoPort bookInfoPort;

    private AddCartItemService(CartPersistencePort cartPersistencePort, BookInfoPort bookInfoPort) {
        this.cartPersistencePort = cartPersistencePort;
        this.bookInfoPort = bookInfoPort;
    }

    public static AddCartItemService create(CartPersistencePort cartPersistencePort, BookInfoPort bookInfoPort) {
        return new AddCartItemService(cartPersistencePort, bookInfoPort);
    }

    @Override
    public CartView execute(AddCartItemCommand command) {
        BookInfo book = bookInfoPort.getBookInfo(command.bookId());
        Cart cart = cartPersistencePort.findByCustomerId(command.customerId())
                .orElseGet(() -> Cart.createFor(command.customerId()));
        cart.addOrUpdateItem(command.bookId(), book.price(), command.quantity(), book.stockQuantity());
        return CartViewAssembler.toView(cartPersistencePort.save(cart), bookInfoPort);
    }
}
