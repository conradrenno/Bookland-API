package com.devrenno.bookland.orders.domain.exception;

import com.devrenno.bookland.orders.domain.entity.OrderStatus;

import java.util.UUID;

public class InvalidOrderStatusTransitionException extends RuntimeException {
    public InvalidOrderStatusTransitionException(UUID orderId, OrderStatus from, OrderStatus to) {
        super("Invalid status transition for order " + orderId + ": " + from + " -> " + to);
    }
}
