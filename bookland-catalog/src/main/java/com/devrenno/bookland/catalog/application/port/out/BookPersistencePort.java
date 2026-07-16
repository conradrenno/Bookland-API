package com.devrenno.bookland.catalog.application.port.out;

import com.devrenno.bookland.catalog.application.common.PageQuery;
import com.devrenno.bookland.catalog.application.common.PageResult;
import com.devrenno.bookland.catalog.application.dto.BookSearchQuery;
import com.devrenno.bookland.catalog.domain.entity.Book;

import java.util.Optional;
import java.util.UUID;

public interface BookPersistencePort {
    Book save(Book book);
    Optional<Book> findById(UUID id);
    Optional<Book> findByIsbn(String isbn);
    boolean existsByIsbn(String isbn);
    PageResult<Book> search(BookSearchQuery query);
    PageResult<Book> findByCategoryId(UUID categoryId, PageQuery pageQuery);
    PageResult<Book> findLowStock(int threshold, PageQuery pageQuery);
}
