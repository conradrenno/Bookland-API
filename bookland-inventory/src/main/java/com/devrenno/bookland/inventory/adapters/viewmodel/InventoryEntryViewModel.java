package com.devrenno.bookland.inventory.adapters.viewmodel;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryEntryViewModel(
        UUID id,
        UUID bookId,
        int previousQuantity,
        int newQuantity,
        int delta,
        String reason,
        UUID adjustedBy,
        LocalDateTime adjustedAt
) {}
