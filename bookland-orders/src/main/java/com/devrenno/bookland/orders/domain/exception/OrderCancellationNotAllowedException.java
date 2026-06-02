package com.devrenno.bookland.orders.domain.exception;

import com.devrenno.bookland.orders.domain.entity.OrderStatus;

import java.util.UUID;

public class OrderCancellationNotAllowedException extends RuntimeException {
    public OrderCancellationNotAllowedException(UUID orderId, OrderStatus currentStatus) {
        super("Order " + orderId + " cannot be cancelled in status: " + currentStatus);
    }
}
