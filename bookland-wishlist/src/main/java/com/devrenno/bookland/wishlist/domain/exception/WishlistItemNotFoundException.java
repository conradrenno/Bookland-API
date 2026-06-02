package com.devrenno.bookland.wishlist.domain.exception;

import java.util.UUID;

public class WishlistItemNotFoundException extends RuntimeException {
    public WishlistItemNotFoundException(UUID customerId, UUID bookId) {
        super("Book " + bookId + " not found in wishlist of customer " + customerId);
    }
}
