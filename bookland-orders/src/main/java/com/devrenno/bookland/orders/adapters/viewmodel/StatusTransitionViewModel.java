package com.devrenno.bookland.orders.adapters.viewmodel;

import com.devrenno.bookland.orders.domain.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record StatusTransitionViewModel(
        OrderStatus fromStatus,
        OrderStatus toStatus,
        LocalDateTime changedAt,
        UUID changedBy
) {}
