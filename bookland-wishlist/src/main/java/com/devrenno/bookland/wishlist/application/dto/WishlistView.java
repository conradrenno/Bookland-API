package com.devrenno.bookland.wishlist.application.dto;

import java.util.List;
import java.util.UUID;

/**
 * Query read-model: a customer's wishlist with each item enriched with book info from the catalog.
 */
public record WishlistView(
        UUID customerId,
        List<WishlistItemView> items
) {}
