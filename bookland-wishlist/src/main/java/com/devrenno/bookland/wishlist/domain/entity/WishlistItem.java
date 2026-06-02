package com.devrenno.bookland.wishlist.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class WishlistItem {
    private final UUID bookId;
    private final LocalDateTime addedAt;
}
