package com.devrenno.bookland.inventory.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Query read-model: a low-stock book enriched with its last inventory movement.
 * Spans catalog book data (via LowStockBooksPort) and inventory history.
 */
public record LowStockBook(
        UUID id,
        String title,
        String isbn,
        String coverImageUrl,
        int stockQuantity,
        LocalDateTime lastMovement
) {}
