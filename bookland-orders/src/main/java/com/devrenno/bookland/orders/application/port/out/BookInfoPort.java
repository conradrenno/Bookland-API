package com.devrenno.bookland.orders.application.port.out;

import com.devrenno.bookland.orders.application.dto.BookInfo;

import java.util.UUID;

public interface BookInfoPort {
    BookInfo getBookInfo(UUID bookId);
}
