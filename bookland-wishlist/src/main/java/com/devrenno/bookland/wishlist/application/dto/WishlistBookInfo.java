package com.devrenno.bookland.wishlist.application.dto;

import java.math.BigDecimal;

/**
 * Wishlist-owned projection of the book data needed to render a wishlist item.
 * Decouples the module from the catalog's application DTOs (the adapter maps catalog data into this).
 */
public record WishlistBookInfo(
        String title,
        String coverImageUrl,
        BigDecimal price,
        int stockQuantity,
        boolean available
) {}
