package com.devrenno.bookland.inventory.adapters.viewmodel;

import java.time.LocalDateTime;
import java.util.UUID;

public record LowStockBookViewModel(
        UUID id,
        String title,
        String isbn,
        int stockQuantity,
        LocalDateTime lastMovement
) {}
