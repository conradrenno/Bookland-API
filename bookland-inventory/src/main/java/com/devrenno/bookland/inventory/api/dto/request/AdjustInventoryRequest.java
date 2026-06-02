package com.devrenno.bookland.inventory.api.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdjustInventoryRequest(
        @NotNull Integer delta,
        String reason
) {}
