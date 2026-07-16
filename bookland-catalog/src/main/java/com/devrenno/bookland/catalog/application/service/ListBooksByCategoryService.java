package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.common.PageQuery;
import com.devrenno.bookland.catalog.application.common.PageResult;
import com.devrenno.bookland.catalog.application.port.in.ListBooksByCategoryUseCase;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.application.port.out.CategoryPersistencePort;
import com.devrenno.bookland.catalog.domain.entity.Book;
import com.devrenno.bookland.catalog.domain.exception.CategoryNotFoundException;

import java.util.UUID;

public class ListBooksByCategoryService implements ListBooksByCategoryUseCase {

    private final BookPersistencePort bookPersistencePort;
    private final CategoryPersistencePort categoryPersistencePort;

    private ListBooksByCategoryService(BookPersistencePort bookPersistencePort,
                                       CategoryPersistencePort categoryPersistencePort) {
        this.bookPersistencePort = bookPersistencePort;
        this.categoryPersistencePort = categoryPersistencePort;
    }

    public static ListBooksByCategoryService create(BookPersistencePort bookPersistencePort,
                                                    CategoryPersistencePort categoryPersistencePort) {
        return new ListBooksByCategoryService(bookPersistencePort, categoryPersistencePort);
    }

    @Override
    public PageResult<Book> execute(UUID categoryId, PageQuery pageQuery) {
        categoryPersistencePort.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));

        return bookPersistencePort.findByCategoryId(categoryId, pageQuery);
    }
}
