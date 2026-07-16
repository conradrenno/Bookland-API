package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.common.PageResult;
import com.devrenno.bookland.catalog.application.dto.BookSearchQuery;
import com.devrenno.bookland.catalog.application.port.in.SearchBooksUseCase;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.domain.entity.Book;

public class SearchBooksService implements SearchBooksUseCase {

    private final BookPersistencePort bookPersistencePort;

    private SearchBooksService(BookPersistencePort bookPersistencePort) {
        this.bookPersistencePort = bookPersistencePort;
    }

    public static SearchBooksService create(BookPersistencePort bookPersistencePort) {
        return new SearchBooksService(bookPersistencePort);
    }

    @Override
    public PageResult<Book> execute(BookSearchQuery query) {
        return bookPersistencePort.search(query);
    }
}
