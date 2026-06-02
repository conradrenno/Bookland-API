package com.devrenno.bookland.orders.application.dto;

import com.devrenno.bookland.orders.domain.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID customerId,
        List<OrderItemResponse> items,
        OrderStatus status,
        BigDecimal totalAmount,
        List<StatusTransitionResponse> statusHistory,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
