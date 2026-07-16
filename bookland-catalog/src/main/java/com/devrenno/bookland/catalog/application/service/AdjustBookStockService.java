package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.port.in.AdjustBookStockUseCase;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.domain.entity.Book;
import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;

import java.util.UUID;

public class AdjustBookStockService implements AdjustBookStockUseCase {

    private final BookPersistencePort bookPersistencePort;

    private AdjustBookStockService(BookPersistencePort bookPersistencePort) {
        this.bookPersistencePort = bookPersistencePort;
    }

    public static AdjustBookStockService create(BookPersistencePort bookPersistencePort) {
        return new AdjustBookStockService(bookPersistencePort);
    }

    @Override
    public int adjustStock(UUID bookId, int delta) {
        Book book = bookPersistencePort.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));
        book.adjustStock(delta);
        return bookPersistencePort.save(book).getStockQuantity();
    }
}
