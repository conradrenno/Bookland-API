package com.devrenno.bookland.orders.adapters.viewmodel;

import com.devrenno.bookland.orders.domain.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderSummaryViewModel(
        UUID id,
        OrderStatus status,
        BigDecimal totalAmount,
        int itemCount,
        LocalDateTime createdAt
) {}
