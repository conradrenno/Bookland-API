package com.devrenno.bookland.inventory.api.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record LowStockBookApiResponse(
        UUID id,
        String title,
        String isbn,
        int stockQuantity,
        LocalDateTime lastMovement
) {}
