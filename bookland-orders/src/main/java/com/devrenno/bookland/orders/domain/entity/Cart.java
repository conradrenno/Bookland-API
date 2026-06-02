package com.devrenno.bookland.orders.domain.entity;

import com.devrenno.bookland.orders.domain.exception.BookNotInCartException;
import com.devrenno.bookland.orders.domain.exception.CartItemUnavailableException;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class Cart {

    private final UUID id;
    private final UUID customerId;
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Cart createFor(UUID customerId) {
        return Cart.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
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
            items.add(new CartItem(bookId, requestedQty, price));
        }
        this.updatedAt = LocalDateTime.now();
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
        this.updatedAt = LocalDateTime.now();
    }

    public void removeItem(UUID bookId) {
        items.removeIf(i -> i.getBookId().equals(bookId));
        this.updatedAt = LocalDateTime.now();
    }

    private CartItem findItem(UUID bookId) {
        return items.stream()
                .filter(i -> i.getBookId().equals(bookId))
                .findFirst()
                .orElse(null);
    }
}
