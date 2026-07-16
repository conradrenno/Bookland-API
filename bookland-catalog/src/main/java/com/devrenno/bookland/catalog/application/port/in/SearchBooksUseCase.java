package com.devrenno.bookland.catalog.application.port.in;

import com.devrenno.bookland.catalog.application.common.PageResult;
import com.devrenno.bookland.catalog.application.dto.BookSearchQuery;
import com.devrenno.bookland.catalog.domain.entity.Book;

public interface SearchBooksUseCase {
    PageResult<Book> execute(BookSearchQuery query);
}
