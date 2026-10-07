package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.port.in.GetBooksByIdsUseCase;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.domain.entity.Book;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class GetBooksByIdsService implements GetBooksByIdsUseCase {

    private final BookPersistencePort bookPersistencePort;

    private GetBooksByIdsService(BookPersistencePort bookPersistencePort) {
        this.bookPersistencePort = bookPersistencePort;
    }

    public static GetBooksByIdsService create(BookPersistencePort bookPersistencePort) {
        return new GetBooksByIdsService(bookPersistencePort);
    }

    /** Same rule as the single lookup: an inactive book is indistinguishable from a missing one. */
    @Override
    public List<Book> execute(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return bookPersistencePort.findAllById(ids).stream().filter(Book::isActive).toList();
    }
}
