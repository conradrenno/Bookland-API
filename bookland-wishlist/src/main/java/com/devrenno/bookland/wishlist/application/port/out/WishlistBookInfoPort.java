package com.devrenno.bookland.wishlist.application.port.out;

import com.devrenno.bookland.catalog.application.dto.BookResponse;

import java.util.UUID;

public interface WishlistBookInfoPort {
    BookResponse getBookInfo(UUID bookId);
}
