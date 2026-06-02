package com.devrenno.bookland.orders.domain.exception;

import java.util.List;
import java.util.UUID;

public class CartItemUnavailableException extends RuntimeException {

    private final List<UUID> unavailableBookIds;

    public CartItemUnavailableException(UUID bookId, int availableStock) {
        super("Insufficient stock for book " + bookId + ": available=" + availableStock);
        this.unavailableBookIds = List.of(bookId);
    }

    public CartItemUnavailableException(List<UUID> unavailableBookIds) {
        super("Insufficient stock for books: " + unavailableBookIds);
        this.unavailableBookIds = unavailableBookIds;
    }

    public List<UUID> getUnavailableBookIds() {
        return unavailableBookIds;
    }
}
