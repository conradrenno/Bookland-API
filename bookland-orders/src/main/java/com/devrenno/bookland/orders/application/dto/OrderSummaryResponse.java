package com.devrenno.bookland.orders.application.dto;

import com.devrenno.bookland.orders.domain.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID id,
        OrderStatus status,
        BigDecimal totalAmount,
        int itemCount,
        LocalDateTime createdAt
) {}
