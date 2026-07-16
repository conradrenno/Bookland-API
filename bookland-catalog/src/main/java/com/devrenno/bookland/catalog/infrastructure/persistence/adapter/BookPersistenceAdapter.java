package com.devrenno.bookland.catalog.infrastructure.persistence.adapter;

import com.devrenno.bookland.catalog.application.common.BookSort;
import com.devrenno.bookland.catalog.application.common.PageQuery;
import com.devrenno.bookland.catalog.application.common.PageResult;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    public PageResult<Book> search(BookSearchQuery query) {
        PageRequest pageable = PageRequest.of(query.page(), query.size(), toSort(query.sort()));
        return toPageResult(bookRepository.findAll(BookSearchSpecification.from(query), pageable)
                .map(mapper::toDomain));
    }

    @Override
    public PageResult<Book> findByCategoryId(UUID categoryId, PageQuery pageQuery) {
        PageRequest pageable = PageRequest.of(pageQuery.page(), pageQuery.size(), Sort.by("title").ascending());
        return toPageResult(bookRepository.findByCategory_IdAndActiveTrue(categoryId, pageable)
                .map(mapper::toDomain));
    }

    @Override
    public PageResult<Book> findLowStock(int threshold, PageQuery pageQuery) {
        PageRequest pageable = PageRequest.of(pageQuery.page(), pageQuery.size(), Sort.by("stockQuantity").ascending());
        return toPageResult(bookRepository.findByStockQuantityLessThanEqualAndActiveTrue(threshold, pageable)
                .map(mapper::toDomain));
    }

    private static Sort toSort(BookSort sort) {
        return switch (sort) {
            case PRICE_ASC -> Sort.by("price").ascending();
            case PRICE_DESC -> Sort.by("price").descending();
            case RATING_DESC -> Sort.by("avgRating").descending();
            case TITLE -> Sort.by("title").ascending();
        };
    }

    private static PageResult<Book> toPageResult(Page<Book> page) {
        return new PageResult<>(
                page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages()
        );
    }
}
