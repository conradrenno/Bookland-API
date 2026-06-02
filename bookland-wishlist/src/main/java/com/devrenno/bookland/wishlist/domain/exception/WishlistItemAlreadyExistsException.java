package com.devrenno.bookland.wishlist.domain.exception;

import java.util.UUID;

public class WishlistItemAlreadyExistsException extends RuntimeException {
    public WishlistItemAlreadyExistsException(UUID customerId, UUID bookId) {
        super("Book " + bookId + " is already in the wishlist of customer " + customerId);
    }
}
