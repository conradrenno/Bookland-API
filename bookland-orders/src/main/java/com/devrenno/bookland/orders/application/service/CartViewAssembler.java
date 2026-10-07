package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.dto.BookInfo;
import com.devrenno.bookland.orders.application.dto.CartItemView;
import com.devrenno.bookland.orders.application.dto.CartView;
import com.devrenno.bookland.orders.application.port.out.BookInfoPort;
import com.devrenno.bookland.orders.application.port.out.CatalogUnavailableException;
import com.devrenno.bookland.orders.domain.entity.Cart;
import com.devrenno.bookland.orders.domain.entity.CartItem;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Assembles a CartView read-model from a domain Cart, enriching each item with book info from the
 * catalog. A book that is no longer in the catalog degrades to an unavailable entry so the cart
 * still renders — checkout is what rejects it.
 */
final class CartViewAssembler {

    private CartViewAssembler() {}

    static CartView toView(Cart cart, BookInfoPort bookInfoPort) {
        Map<UUID, BookInfo> books = lookUp(cart, bookInfoPort);
        List<CartItemView> items = cart.getItems().stream()
                .map(item -> Optional.ofNullable(books.get(item.getBookId()))
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

    /**
     * One call for the whole cart. With the catalog unreachable every item renders as unavailable
     * rather than the cart failing: the customer still sees what is in it.
     */
    private static Map<UUID, BookInfo> lookUp(Cart cart, BookInfoPort bookInfoPort) {
        try {
            return bookInfoPort.findBookInfos(cart.getItems().stream().map(CartItem::getBookId).toList());
        } catch (CatalogUnavailableException e) {
            return Map.of();
        }
    }
}
