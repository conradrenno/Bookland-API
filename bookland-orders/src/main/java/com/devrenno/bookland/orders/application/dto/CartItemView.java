package com.devrenno.bookland.orders.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Query read-model: a cart item enriched with the catalog data needed to render it
 * (title, cover and current availability). The price is the one snapshotted when the item
 * was added to the cart, not the catalog's current price.
 */
public record CartItemView(
        UUID bookId,
        String title,
        String coverImageUrl,
        int quantity,
        BigDecimal unitPrice,
        boolean available
) {}
