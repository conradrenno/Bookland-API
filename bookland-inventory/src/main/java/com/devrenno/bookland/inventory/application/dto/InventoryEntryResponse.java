package com.devrenno.bookland.inventory.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryEntryResponse(
        UUID id,
        UUID bookId,
        int previousQuantity,
        int newQuantity,
        int delta,
        String reason,
        UUID adjustedBy,
        LocalDateTime adjustedAt
) {}
