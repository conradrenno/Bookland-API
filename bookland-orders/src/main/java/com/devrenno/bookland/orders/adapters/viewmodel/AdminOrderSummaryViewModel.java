package com.devrenno.bookland.orders.adapters.viewmodel;

import com.devrenno.bookland.orders.domain.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Admin listing row: same shape as OrderSummaryViewModel plus the owning customer, which the
 * customer-facing summary deliberately omits.
 */
public record AdminOrderSummaryViewModel(
        UUID id,
        UUID customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        int itemCount,
        LocalDateTime createdAt
) {}
