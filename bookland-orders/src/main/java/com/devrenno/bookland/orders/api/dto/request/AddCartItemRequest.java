package com.devrenno.bookland.orders.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddCartItemRequest(
        @NotNull UUID bookId,
        @Min(1) int quantity
) {}
