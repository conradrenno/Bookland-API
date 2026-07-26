package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.dto.CartItemView;
import com.devrenno.bookland.orders.application.dto.CartView;
import com.devrenno.bookland.orders.application.port.out.BookInfoPort;
import com.devrenno.bookland.orders.domain.entity.Cart;

import java.util.List;

/**
 * Assembles a CartView read-model from a domain Cart, enriching each item with book info from the
 * catalog. A book that is no longer in the catalog degrades to an unavailable entry so the cart
 * still renders — checkout is what rejects it.
 */
final class CartViewAssembler {

    private CartViewAssembler() {}

    static CartView toView(Cart cart, BookInfoPort bookInfoPort) {
        List<CartItemView> items = cart.getItems().stream()
                .map(item -> bookInfoPort.findBookInfo(item.getBookId())
                        .map(book -> new CartItemView(
                                item.getBookId(), book.title(), book.coverImageUrl(),
                                item.getQuantity(), item.getUnitPriceAtAddition(),
                                book.stockQuantity() >= item.getQuantity()
                        ))
                        .orElseGet(() -> new CartItemView(
                                item.getBookId(), "Unavailable", null,
                                item.getQuantity(), item.getUnitPriceAtAddition(), false
                        )))
                .toList();
        return new CartView(cart.getId(), cart.getCustomerId(), items, cart.getUpdatedAt());
    }
}
