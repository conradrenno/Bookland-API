package com.devrenno.bookland.wishlist.adapters.viewmodel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WishlistItemViewModel(
        UUID bookId,
        String title,
        String coverImageUrl,
        BigDecimal price,
        int stockQuantity,
        boolean available,
        LocalDateTime addedAt
) {}
