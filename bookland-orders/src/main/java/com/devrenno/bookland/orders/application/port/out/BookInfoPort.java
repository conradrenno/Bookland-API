package com.devrenno.bookland.orders.application.port.out;

import com.devrenno.bookland.orders.application.dto.BookInfo;

import java.util.Optional;
import java.util.UUID;

public interface BookInfoPort {
    /** Throws when the book is unknown to the catalog — for flows that must reject an unknown book. */
    BookInfo getBookInfo(UUID bookId);

    /**
     * Empty when the book is unknown to the catalog (never listed, or soft-deleted since). Used by
     * the flows that must keep working around a book that vanished: rendering the cart and checking
     * out (where it counts as an unavailable item, not a 404).
     */
    Optional<BookInfo> findBookInfo(UUID bookId);
}
