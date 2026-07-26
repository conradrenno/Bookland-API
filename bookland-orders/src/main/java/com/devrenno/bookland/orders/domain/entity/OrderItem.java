package com.devrenno.bookland.orders.domain.entity;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class OrderItem {

    private final UUID bookId;
    private final String title;
    /** Snapshot of the book's cover at purchase time, like the title — survives catalog changes. */
    private final String coverImageUrl;
    private final int quantity;
    private final BigDecimal unitPrice;

    private OrderItem(UUID bookId, String title, String coverImageUrl, int quantity, BigDecimal unitPrice) {
        this.bookId = bookId;
        this.title = title;
        this.coverImageUrl = coverImageUrl;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public static OrderItem of(UUID bookId, String title, String coverImageUrl,
                               int quantity, BigDecimal unitPrice) {
        return new OrderItem(bookId, title, coverImageUrl, quantity, unitPrice);
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
