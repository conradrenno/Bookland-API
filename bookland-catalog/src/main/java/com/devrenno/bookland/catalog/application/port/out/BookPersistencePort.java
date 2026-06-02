package com.devrenno.bookland.catalog.application.port.out;

import com.devrenno.bookland.catalog.application.dto.BookSearchQuery;
import com.devrenno.bookland.catalog.domain.entity.Book;
import com.devrenno.bookland.catalog.domain.valueobject.BookId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface BookPersistencePort {
    Book save(Book book);
    Optional<Book> findById(UUID id);
    Optional<Book> findByIsbn(String isbn);
    boolean existsByIsbn(String isbn);
    Page<Book> search(BookSearchQuery query);
    Page<Book> findByCategoryId(UUID categoryId, Pageable pageable);
    Page<Book> findLowStock(int threshold, Pageable pageable);
}
