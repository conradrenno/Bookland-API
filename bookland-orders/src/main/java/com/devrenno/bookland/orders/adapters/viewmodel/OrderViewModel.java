package com.devrenno.bookland.orders.adapters.viewmodel;

import com.devrenno.bookland.orders.domain.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderViewModel(
        UUID id,
        UUID customerId,
        List<OrderItemViewModel> items,
        OrderStatus status,
        BigDecimal totalAmount,
        List<StatusTransitionViewModel> statusHistory,
        Instant createdAt,
        Instant updatedAt
) {}
