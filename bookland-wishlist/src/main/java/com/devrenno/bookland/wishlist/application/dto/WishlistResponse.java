package com.devrenno.bookland.wishlist.application.dto;

import java.util.List;
import java.util.UUID;

public record WishlistResponse(
        UUID customerId,
        List<WishlistItemResponse> items
) {}
