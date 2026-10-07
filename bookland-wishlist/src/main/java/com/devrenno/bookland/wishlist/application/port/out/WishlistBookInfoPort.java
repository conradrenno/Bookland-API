package com.devrenno.bookland.wishlist.application.port.out;

import com.devrenno.bookland.wishlist.application.dto.WishlistBookInfo;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * What the wishlist needs to know about books, from the catalog. Both methods throw
 * {@link CatalogUnavailableException} when the catalog cannot answer.
 */
public interface WishlistBookInfoPort {

    /** Throws {@code BookNotFoundException} when the catalog does not have the book. */
    WishlistBookInfo getBookInfo(UUID bookId);

    /** Many books in one call; a book the catalog does not have is absent from the map. */
    Map<UUID, WishlistBookInfo> findBookInfos(Collection<UUID> bookIds);
}
