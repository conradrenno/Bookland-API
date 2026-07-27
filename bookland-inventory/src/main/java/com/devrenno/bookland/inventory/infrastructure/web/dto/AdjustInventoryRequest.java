package com.devrenno.bookland.inventory.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdjustInventoryRequest(
        @NotNull Integer delta,
        // inventory_entries.reason is varchar(255).
        @Size(max = 255) String reason
) {}
