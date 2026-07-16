package com.devrenno.bookland.wishlist.application.port.in;

import com.devrenno.bookland.wishlist.application.dto.WishlistView;

import java.util.UUID;

public interface GetWishlistUseCase {
    WishlistView execute(UUID customerId);
}
