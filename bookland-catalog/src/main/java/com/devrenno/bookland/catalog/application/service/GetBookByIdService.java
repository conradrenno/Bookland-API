package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.port.in.GetBookByIdUseCase;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.domain.entity.Book;
import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;

import java.util.UUID;

public class GetBookByIdService implements GetBookByIdUseCase {

    private final BookPersistencePort bookPersistencePort;

    private GetBookByIdService(BookPersistencePort bookPersistencePort) {
        this.bookPersistencePort = bookPersistencePort;
    }

    public static GetBookByIdService create(BookPersistencePort bookPersistencePort) {
        return new GetBookByIdService(bookPersistencePort);
    }

    @Override
    public Book execute(UUID bookId) {
        return bookPersistencePort.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));
    }
}
