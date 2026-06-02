package com.devrenno.bookland.inventory.application.dto;

import java.util.UUID;

public record AdjustInventoryCommand(
        UUID bookId,
        int delta,
        String reason,
        UUID adjustedBy
) {}
