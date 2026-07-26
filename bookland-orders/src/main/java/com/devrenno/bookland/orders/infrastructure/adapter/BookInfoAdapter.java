package com.devrenno.bookland.orders.infrastructure.adapter;

import com.devrenno.bookland.catalog.application.port.in.GetBookByIdUseCase;
import com.devrenno.bookland.catalog.domain.entity.Book;
import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;
import com.devrenno.bookland.orders.application.dto.BookInfo;
import com.devrenno.bookland.orders.application.port.out.BookInfoPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BookInfoAdapter implements BookInfoPort {

    private final GetBookByIdUseCase getBookByIdUseCase;

    @Override
    public BookInfo getBookInfo(UUID bookId) {
        return toInfo(getBookByIdUseCase.execute(bookId));
    }

    @Override
    public Optional<BookInfo> findBookInfo(UUID bookId) {
        try {
            return Optional.of(toInfo(getBookByIdUseCase.execute(bookId)));
        } catch (BookNotFoundException e) {
            return Optional.empty();
        }
    }

    private static BookInfo toInfo(Book book) {
        return new BookInfo(
                book.getId().value(), book.getTitle(), book.getCoverImageUrl(),
                book.getPrice().value(), book.getStockQuantity());
    }
}
