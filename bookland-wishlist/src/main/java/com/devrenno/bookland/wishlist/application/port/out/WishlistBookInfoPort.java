package com.devrenno.bookland.wishlist.application.port.out;

import com.devrenno.bookland.wishlist.application.dto.WishlistBookInfo;

import java.util.UUID;

public interface WishlistBookInfoPort {
    WishlistBookInfo getBookInfo(UUID bookId);
}
