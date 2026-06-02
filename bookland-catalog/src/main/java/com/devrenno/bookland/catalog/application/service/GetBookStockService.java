package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.annotation.UseCase;
import com.devrenno.bookland.catalog.application.port.in.GetBookStockUseCase;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class GetBookStockService implements GetBookStockUseCase {

    private final BookPersistencePort bookPersistencePort;

    @Override
    public int getStock(UUID bookId) {
        return bookPersistencePort.findById(bookId)
                .map(book -> book.getStockQuantity())
                .orElseThrow(() -> new BookNotFoundException(bookId));
    }
}
