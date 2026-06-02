package com.devrenno.bookland.wishlist.application.service;

import com.devrenno.bookland.catalog.application.dto.BookResponse;
import com.devrenno.bookland.wishlist.application.dto.WishlistItemResponse;
import com.devrenno.bookland.wishlist.application.dto.WishlistResponse;
import com.devrenno.bookland.wishlist.application.port.out.WishlistBookInfoPort;
import com.devrenno.bookland.wishlist.domain.entity.Wishlist;

import java.util.List;

final class WishlistResponseMapper {

    private WishlistResponseMapper() {}

    static WishlistResponse toResponse(Wishlist wishlist, WishlistBookInfoPort bookInfoPort) {
        List<WishlistItemResponse> items = wishlist.getItems().stream()
                .map(item -> {
                    try {
                        BookResponse book = bookInfoPort.getBookInfo(item.getBookId());
                        return new WishlistItemResponse(
                                item.getBookId(), book.title(), book.price(),
                                book.stockQuantity(), book.available(), item.getAddedAt()
                        );
                    } catch (Exception e) {
                        return new WishlistItemResponse(
                                item.getBookId(), "Unavailable", null, 0, false, item.getAddedAt()
                        );
                    }
                })
                .toList();
        return new WishlistResponse(wishlist.getCustomerId(), items);
    }
}
