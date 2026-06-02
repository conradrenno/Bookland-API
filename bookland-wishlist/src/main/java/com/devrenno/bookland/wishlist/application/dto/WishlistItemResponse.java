package com.devrenno.bookland.wishlist.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WishlistItemResponse(
        UUID bookId,
        String title,
        BigDecimal price,
        int stockQuantity,
        boolean available,
        LocalDateTime addedAt
) {}
