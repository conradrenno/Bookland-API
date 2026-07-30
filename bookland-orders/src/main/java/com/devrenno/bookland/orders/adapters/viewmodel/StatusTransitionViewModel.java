package com.devrenno.bookland.orders.adapters.viewmodel;

import com.devrenno.bookland.orders.domain.entity.OrderStatus;

import java.time.Instant;
import java.util.UUID;

public record StatusTransitionViewModel(
        OrderStatus fromStatus,
        OrderStatus toStatus,
        Instant changedAt,
        UUID changedBy
) {}
