package com.devrenno.bookland.inventory.adapters.viewmodel;

import java.time.Instant;
import java.util.UUID;

public record LowStockBookViewModel(
        UUID id,
        String title,
        String isbn,
        String coverImageUrl,
        int stockQuantity,
        Instant lastMovement
) {}
