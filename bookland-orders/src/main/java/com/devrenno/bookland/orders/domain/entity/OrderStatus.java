package com.devrenno.bookland.orders.domain.entity;

import java.util.Set;

/**
 * The checkout runs as a saga: {@code PENDING} while the stock is being reserved, then
 * {@code AWAITING_PAYMENT} while it is charged. {@code REJECTED} (no stock) and
 * {@code PAYMENT_FAILED} (declined) are its two failures; {@code CONFIRMED} its success.
 */
public enum OrderStatus {
    PENDING, AWAITING_PAYMENT, CONFIRMED, SHIPPED, DELIVERED, CANCELLED, PAYMENT_FAILED, REJECTED;

    /** Statuses of orders still in progress (not yet delivered nor terminated). */
    private static final Set<OrderStatus> ACTIVE = Set.of(PENDING, AWAITING_PAYMENT, CONFIRMED, SHIPPED);

    public boolean isActive() {
        return ACTIVE.contains(this);
    }

    public static Set<OrderStatus> activeStatuses() {
        return ACTIVE;
    }
}
