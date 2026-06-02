package com.devrenno.bookland.inventory.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record LowStockBookResponse(
        UUID id,
        String title,
        String isbn,
        int stockQuantity,
        LocalDateTime lastMovement
) {}
