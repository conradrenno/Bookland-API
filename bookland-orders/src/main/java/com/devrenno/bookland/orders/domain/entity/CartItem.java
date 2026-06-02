package com.devrenno.bookland.orders.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CartItem {

    private final UUID bookId;
    private int quantity;
    private final BigDecimal unitPriceAtAddition;

    void updateQuantity(int quantity) {
        this.quantity = quantity;
    }
}
