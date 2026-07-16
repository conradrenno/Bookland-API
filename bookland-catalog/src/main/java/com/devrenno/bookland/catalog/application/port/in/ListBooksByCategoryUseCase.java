package com.devrenno.bookland.catalog.application.port.in;

import com.devrenno.bookland.catalog.application.common.PageQuery;
import com.devrenno.bookland.catalog.application.common.PageResult;
import com.devrenno.bookland.catalog.domain.entity.Book;

import java.util.UUID;

public interface ListBooksByCategoryUseCase {
    PageResult<Book> execute(UUID categoryId, PageQuery pageQuery);
}
