package com.devrenno.bookland.wishlist.application.service;

import com.devrenno.bookland.wishlist.application.dto.WishlistBookInfo;
import com.devrenno.bookland.wishlist.application.dto.WishlistItemView;
import com.devrenno.bookland.wishlist.application.dto.WishlistView;
import com.devrenno.bookland.wishlist.application.port.out.CatalogUnavailableException;
import com.devrenno.bookland.wishlist.application.port.out.WishlistBookInfoPort;
import com.devrenno.bookland.wishlist.domain.entity.Wishlist;
import com.devrenno.bookland.wishlist.domain.entity.WishlistItem;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Assembles a WishlistView read-model from a domain Wishlist, enriching each item with book info
 * from the catalog. Unavailable books degrade gracefully to a placeholder entry.
 */
final class WishlistViewAssembler {

    private WishlistViewAssembler() {}

    static WishlistView toView(Wishlist wishlist, WishlistBookInfoPort bookInfoPort) {
        Map<UUID, WishlistBookInfo> books = lookUp(wishlist, bookInfoPort);
        List<WishlistItemView> items = wishlist.getItems().stream()
                .map(item -> {
                    WishlistBookInfo book = books.get(item.getBookId());
                    if (book == null) {
                        return new WishlistItemView(
                                item.getBookId(), "Unavailable", null, null, 0, false, item.getAddedAt()
                        );
                    }
                    return new WishlistItemView(
                            item.getBookId(), book.title(), book.coverImageUrl(), book.price(),
                            book.stockQuantity(), book.available(), item.getAddedAt()
                    );
                })
                .toList();
        return new WishlistView(wishlist.getCustomerId(), items);
    }

    /** One call for the whole wishlist; with the catalog unreachable, every item renders unavailable. */
    private static Map<UUID, WishlistBookInfo> lookUp(Wishlist wishlist, WishlistBookInfoPort bookInfoPort) {
        try {
            return bookInfoPort.findBookInfos(wishlist.getItems().stream().map(WishlistItem::getBookId).toList());
        } catch (CatalogUnavailableException e) {
            return Map.of();
        }
    }
}
