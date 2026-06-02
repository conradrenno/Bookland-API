package com.devrenno.bookland.orders.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID id,
        UUID customerId,
        List<CartItemResponse> items,
        BigDecimal total,
        LocalDateTime updatedAt
) {}
