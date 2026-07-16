package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.common.PageQuery;
import com.devrenno.bookland.catalog.application.common.PageResult;
import com.devrenno.bookland.catalog.application.port.in.GetLowStockBooksUseCase;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.domain.entity.Book;

public class GetLowStockBooksService implements GetLowStockBooksUseCase {

    private final BookPersistencePort bookPersistencePort;

    private GetLowStockBooksService(BookPersistencePort bookPersistencePort) {
        this.bookPersistencePort = bookPersistencePort;
    }

    public static GetLowStockBooksService create(BookPersistencePort bookPersistencePort) {
        return new GetLowStockBooksService(bookPersistencePort);
    }

    @Override
    public PageResult<Book> execute(int threshold, PageQuery pageQuery) {
        return bookPersistencePort.findLowStock(threshold, pageQuery);
    }
}
