package com.devrenno.bookland.orders.domain.entity;

import com.devrenno.bookland.orders.domain.exception.BookNotInCartException;
import com.devrenno.bookland.orders.domain.exception.CartItemUnavailableException;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class Cart {

    private final UUID id;
    private final UUID customerId;
    private final List<CartItem> items;
    private final Instant createdAt;
    private Instant updatedAt;

    private Cart(UUID id, UUID customerId, List<CartItem> items,
                 Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.items = items;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Cart createFor(UUID customerId) {
        Instant now = Instant.now();
        return new Cart(UUID.randomUUID(), customerId, new ArrayList<>(), now, now);
    }

    public static Cart reconstitute(UUID id, UUID customerId, List<CartItem> items,
                                    Instant createdAt, Instant updatedAt) {
        return new Cart(id, customerId, new ArrayList<>(items), createdAt, updatedAt);
    }

    public void addOrUpdateItem(UUID bookId, BigDecimal price, int requestedQty, int availableStock) {
        CartItem existing = findItem(bookId);
        if (existing != null) {
            int newQty = existing.getQuantity() + requestedQty;
            if (newQty > availableStock) {
                throw new CartItemUnavailableException(bookId, availableStock);
            }
            existing.updateQuantity(newQty);
        } else {
            if (requestedQty > availableStock) {
                throw new CartItemUnavailableException(bookId, availableStock);
            }
            items.add(CartItem.of(bookId, requestedQty, price));
        }
        this.updatedAt = Instant.now();
    }

    public void updateItemQuantity(UUID bookId, int newQty, int availableStock) {
        CartItem item = findItem(bookId);
        if (item == null) throw new BookNotInCartException(bookId);
        if (newQty == 0) {
            items.removeIf(i -> i.getBookId().equals(bookId));
        } else {
            if (newQty > availableStock) throw new CartItemUnavailableException(bookId, availableStock);
            item.updateQuantity(newQty);
        }
        this.updatedAt = Instant.now();
    }

    public void removeItem(UUID bookId) {
        items.removeIf(i -> i.getBookId().equals(bookId));
        this.updatedAt = Instant.now();
    }

    private CartItem findItem(UUID bookId) {
        return items.stream()
                .filter(i -> i.getBookId().equals(bookId))
                .findFirst()
                .orElse(null);
    }
}
