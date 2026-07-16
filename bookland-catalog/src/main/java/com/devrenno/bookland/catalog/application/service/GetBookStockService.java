package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.port.in.GetBookStockUseCase;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;

import java.util.UUID;

public class GetBookStockService implements GetBookStockUseCase {

    private final BookPersistencePort bookPersistencePort;

    private GetBookStockService(BookPersistencePort bookPersistencePort) {
        this.bookPersistencePort = bookPersistencePort;
    }

    public static GetBookStockService create(BookPersistencePort bookPersistencePort) {
        return new GetBookStockService(bookPersistencePort);
    }

    @Override
    public int getStock(UUID bookId) {
        return bookPersistencePort.findById(bookId)
                .map(book -> book.getStockQuantity())
                .orElseThrow(() -> new BookNotFoundException(bookId));
    }
}
