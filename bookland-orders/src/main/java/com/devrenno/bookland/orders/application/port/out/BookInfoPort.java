package com.devrenno.bookland.orders.application.port.out;

import com.devrenno.bookland.orders.application.dto.BookInfo;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * What orders needs to know about books, from the catalog. Both methods throw
 * {@link CatalogUnavailableException} when the catalog cannot answer.
 */
public interface BookInfoPort {

    /** For flows that must reject an unknown book: throws {@code BookNotFoundException} when there is none. */
    BookInfo getBookInfo(UUID bookId);

    /**
     * Many books in one call — a cart is one round trip, not one per item. A book unknown to the
     * catalog (never listed, or soft-deleted since) is simply absent from the map: the cart renders it
     * as unavailable and the checkout counts it as an unavailable item, not a 404.
     */
    Map<UUID, BookInfo> findBookInfos(Collection<UUID> bookIds);
}
