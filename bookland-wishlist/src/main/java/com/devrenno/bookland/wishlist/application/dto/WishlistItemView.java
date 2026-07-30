package com.devrenno.bookland.wishlist.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WishlistItemView(
        UUID bookId,
        String title,
        String coverImageUrl,
        BigDecimal price,
        int stockQuantity,
        boolean available,
        Instant addedAt
) {}
