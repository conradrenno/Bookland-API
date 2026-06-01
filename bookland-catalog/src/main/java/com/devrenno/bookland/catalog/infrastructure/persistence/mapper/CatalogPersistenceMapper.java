package com.devrenno.bookland.catalog.infrastructure.persistence.mapper;

import com.devrenno.bookland.catalog.domain.entity.Book;
import com.devrenno.bookland.catalog.domain.entity.Category;
import com.devrenno.bookland.catalog.infrastructure.persistence.entity.BookJpaEntity;
import com.devrenno.bookland.catalog.infrastructure.persistence.entity.CategoryJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring",
        imports = {
                com.devrenno.bookland.catalog.domain.valueobject.BookId.class,
                com.devrenno.bookland.catalog.domain.valueobject.CategoryId.class,
                com.devrenno.bookland.catalog.domain.valueobject.ISBN.class,
                com.devrenno.bookland.catalog.domain.valueobject.Price.class
        })
public interface CatalogPersistenceMapper {

    @Mapping(target = "id", expression = "java(BookId.of(entity.getId()))")
    @Mapping(target = "isbn", expression = "java(ISBN.of(entity.getIsbn()))")
    @Mapping(target = "price", expression = "java(Price.of(entity.getPrice()))")
    @Mapping(target = "categoryId", expression = "java(CategoryId.of(entity.getCategory().getId()))")
    Book toDomain(BookJpaEntity entity);

    @Mapping(target = "id", expression = "java(book.getId().value())")
    @Mapping(target = "isbn", expression = "java(book.getIsbn().value())")
    @Mapping(target = "price", expression = "java(book.getPrice().value())")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "active", source = "book.active")
    @Mapping(target = "title", source = "book.title")
    @Mapping(target = "authors", source = "book.authors")
    @Mapping(target = "publisher", source = "book.publisher")
    @Mapping(target = "publicationYear", source = "book.publicationYear")
    @Mapping(target = "edition", source = "book.edition")
    @Mapping(target = "synopsis", source = "book.synopsis")
    @Mapping(target = "stockQuantity", source = "book.stockQuantity")
    @Mapping(target = "avgRating", source = "book.avgRating")
    @Mapping(target = "createdAt", source = "book.createdAt")
    @Mapping(target = "updatedAt", source = "book.updatedAt")
    BookJpaEntity toEntity(Book book, CategoryJpaEntity category);

    @Mapping(target = "id", expression = "java(CategoryId.of(entity.getId()))")
    Category toDomain(CategoryJpaEntity entity);
}
