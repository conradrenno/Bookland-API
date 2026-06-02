package com.devrenno.bookland.orders.domain.entity;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class StatusTransition {

    private final UUID id;
    private final UUID orderId;
    private final OrderStatus fromStatus;
    private final OrderStatus toStatus;
    private final LocalDateTime changedAt;
    private final UUID changedBy;

    public static StatusTransition create(UUID orderId, OrderStatus from, OrderStatus to, UUID changedBy) {
        return StatusTransition.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .fromStatus(from)
                .toStatus(to)
                .changedAt(LocalDateTime.now())
                .changedBy(changedBy)
                .build();
    }
}
