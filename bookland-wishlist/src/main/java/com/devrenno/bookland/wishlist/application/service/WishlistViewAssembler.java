package com.devrenno.bookland.wishlist.application.service;

import com.devrenno.bookland.wishlist.application.dto.WishlistBookInfo;
import com.devrenno.bookland.wishlist.application.dto.WishlistItemView;
import com.devrenno.bookland.wishlist.application.dto.WishlistView;
import com.devrenno.bookland.wishlist.application.port.out.WishlistBookInfoPort;
import com.devrenno.bookland.wishlist.domain.entity.Wishlist;

import java.util.List;

/**
 * Assembles a WishlistView read-model from a domain Wishlist, enriching each item with book info
 * from the catalog. Unavailable books degrade gracefully to a placeholder entry.
 */
final class WishlistViewAssembler {

    private WishlistViewAssembler() {}

    static WishlistView toView(Wishlist wishlist, WishlistBookInfoPort bookInfoPort) {
        List<WishlistItemView> items = wishlist.getItems().stream()
                .map(item -> {
                    try {
                        WishlistBookInfo book = bookInfoPort.getBookInfo(item.getBookId());
                        return new WishlistItemView(
                                item.getBookId(), book.title(), book.coverImageUrl(), book.price(),
                                book.stockQuantity(), book.available(), item.getAddedAt()
                        );
                    } catch (Exception e) {
                        return new WishlistItemView(
                                item.getBookId(), "Unavailable", null, null, 0, false, item.getAddedAt()
                        );
                    }
                })
                .toList();
        return new WishlistView(wishlist.getCustomerId(), items);
    }
}
