package com.devrenno.bookland.orders.infrastructure.adapter;

import com.devrenno.bookland.catalog.application.port.in.GetBookByIdUseCase;
import com.devrenno.bookland.orders.application.dto.BookInfo;
import com.devrenno.bookland.orders.application.port.out.BookInfoPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BookInfoAdapter implements BookInfoPort {

    private final GetBookByIdUseCase getBookByIdUseCase;

    @Override
    public BookInfo getBookInfo(UUID bookId) {
        var book = getBookByIdUseCase.execute(bookId);
        return new BookInfo(
                book.getId().value(), book.getTitle(), book.getPrice().value(), book.getStockQuantity());
    }
}
