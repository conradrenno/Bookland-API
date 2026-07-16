package com.devrenno.bookland.inventory.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

public record AdjustInventoryRequest(
        @NotNull Integer delta,
        String reason
) {}
