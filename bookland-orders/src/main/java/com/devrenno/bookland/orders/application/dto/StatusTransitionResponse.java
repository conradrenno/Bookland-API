package com.devrenno.bookland.orders.application.dto;

import com.devrenno.bookland.orders.domain.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record StatusTransitionResponse(
        OrderStatus fromStatus,
        OrderStatus toStatus,
        LocalDateTime changedAt,
        UUID changedBy
) {}
