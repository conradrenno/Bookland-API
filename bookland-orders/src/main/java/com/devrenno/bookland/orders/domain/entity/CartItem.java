package com.devrenno.bookland.orders.domain.entity;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class CartItem {

    private final UUID bookId;
    private int quantity;
    private final BigDecimal unitPriceAtAddition;

    private CartItem(UUID bookId, int quantity, BigDecimal unitPriceAtAddition) {
        this.bookId = bookId;
        this.quantity = quantity;
        this.unitPriceAtAddition = unitPriceAtAddition;
    }

    public static CartItem of(UUID bookId, int quantity, BigDecimal unitPriceAtAddition) {
        return new CartItem(bookId, quantity, unitPriceAtAddition);
    }

    void updateQuantity(int quantity) {
        this.quantity = quantity;
    }
}
