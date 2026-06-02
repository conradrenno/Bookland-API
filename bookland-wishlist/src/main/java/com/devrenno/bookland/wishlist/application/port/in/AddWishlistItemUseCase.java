package com.devrenno.bookland.wishlist.application.port.in;

import com.devrenno.bookland.wishlist.application.dto.WishlistResponse;

import java.util.UUID;

public interface AddWishlistItemUseCase {
    WishlistResponse execute(UUID customerId, UUID bookId);
}
