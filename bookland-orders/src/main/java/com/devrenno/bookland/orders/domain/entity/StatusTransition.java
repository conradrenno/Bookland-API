package com.devrenno.bookland.orders.domain.entity;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class StatusTransition {

    private final UUID id;
    private final UUID orderId;
    private final OrderStatus fromStatus;
    private final OrderStatus toStatus;
    private final Instant changedAt;
    private final UUID changedBy;

    private StatusTransition(UUID id, UUID orderId, OrderStatus fromStatus, OrderStatus toStatus,
                             Instant changedAt, UUID changedBy) {
        this.id = id;
        this.orderId = orderId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedAt = changedAt;
        this.changedBy = changedBy;
    }

    public static StatusTransition create(UUID orderId, OrderStatus from, OrderStatus to, UUID changedBy) {
        return new StatusTransition(UUID.randomUUID(), orderId, from, to, Instant.now(), changedBy);
    }

    public static StatusTransition reconstitute(UUID id, UUID orderId, OrderStatus from, OrderStatus to,
                                                Instant changedAt, UUID changedBy) {
        return new StatusTransition(id, orderId, from, to, changedAt, changedBy);
    }
}
