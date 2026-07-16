package com.devrenno.bookland.orders.domain.entity;

import java.util.Set;

public enum OrderStatus {
    AWAITING_PAYMENT, CONFIRMED, SHIPPED, DELIVERED, CANCELLED, PAYMENT_FAILED;

    /** Statuses of orders still in progress (not yet delivered nor terminated). */
    private static final Set<OrderStatus> ACTIVE = Set.of(AWAITING_PAYMENT, CONFIRMED, SHIPPED);

    public boolean isActive() {
        return ACTIVE.contains(this);
    }

    public static Set<OrderStatus> activeStatuses() {
        return ACTIVE;
    }
}
