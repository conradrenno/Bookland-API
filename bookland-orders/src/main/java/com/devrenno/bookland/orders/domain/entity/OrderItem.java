package com.devrenno.bookland.orders.domain.entity;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class OrderItem {

    private final UUID bookId;
    private final String title;
    private final int quantity;
    private final BigDecimal unitPrice;

    private OrderItem(UUID bookId, String title, int quantity, BigDecimal unitPrice) {
        this.bookId = bookId;
        this.title = title;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public static OrderItem of(UUID bookId, String title, int quantity, BigDecimal unitPrice) {
        return new OrderItem(bookId, title, quantity, unitPrice);
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
