package com.devrenno.bookland.orders.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Query read-model: a cart whose items carry the catalog data needed to render them.
 */
public record CartView(
        UUID id,
        UUID customerId,
        List<CartItemView> items,
        Instant updatedAt
) {}
