package com.devrenno.bookland.orders.infrastructure.web.dto;

import jakarta.validation.constraints.Min;

public record UpdateCartItemRequest(
        @Min(0) int quantity
) {}
