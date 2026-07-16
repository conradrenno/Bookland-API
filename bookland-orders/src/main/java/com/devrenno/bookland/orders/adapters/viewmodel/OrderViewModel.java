package com.devrenno.bookland.orders.adapters.viewmodel;

import com.devrenno.bookland.orders.domain.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderViewModel(
        UUID id,
        UUID customerId,
        List<OrderItemViewModel> items,
        OrderStatus status,
        BigDecimal totalAmount,
        List<StatusTransitionViewModel> statusHistory,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
