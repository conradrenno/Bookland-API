package com.devrenno.bookland.catalog.infrastructure.persistence.adapter;

import com.devrenno.bookland.catalog.application.dto.BookSearchQuery;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.domain.entity.Book;
import com.devrenno.bookland.catalog.infrastructure.persistence.entity.BookJpaEntity;
import com.devrenno.bookland.catalog.infrastructure.persistence.entity.CategoryJpaEntity;
import com.devrenno.bookland.catalog.infrastructure.persistence.mapper.CatalogPersistenceMapper;
import com.devrenno.bookland.catalog.infrastructure.persistence.repository.BookJpaRepository;
import com.devrenno.bookland.catalog.infrastructure.persistence.repository.CategoryJpaRepository;
import com.devrenno.bookland.catalog.infrastructure.persistence.specification.BookSearchSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BookPersistenceAdapter implements BookPersistencePort {

    private final BookJpaRepository bookRepository;
    private final CategoryJpaRepository categoryRepository;
    private final CatalogPersistenceMapper mapper;

    @Override
    public Book save(Book book) {
        CategoryJpaEntity category = categoryRepository.getReferenceById(book.getCategoryId().value());
        BookJpaEntity entity = mapper.toEntity(book, category);
        return mapper.toDomain(bookRepository.save(entity));
    }

    @Override
    public Optional<Book> findById(UUID id) {
        return bookRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Book> findByIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn).map(mapper::toDomain);
    }

    @Override
    public boolean existsByIsbn(String isbn) {
        return bookRepository.existsByIsbn(isbn);
    }

    @Override
    public Page<Book> search(BookSearchQuery query) {
        return bookRepository.findAll(BookSearchSpecification.from(query), query.pageable())
                .map(mapper::toDomain);
    }

    @Override
    public Page<Book> findByCategoryId(UUID categoryId, Pageable pageable) {
        return bookRepository.findByCategory_IdAndActiveTrue(categoryId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<Book> findLowStock(int threshold, Pageable pageable) {
        return bookRepository.findByStockQuantityLessThanEqualAndActiveTrue(threshold, pageable)
                .map(mapper::toDomain);
    }
}
