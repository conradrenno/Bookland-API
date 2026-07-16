package com.devrenno.bookland.catalog.infrastructure.persistence.mapper;

import com.devrenno.bookland.catalog.domain.entity.Book;
import com.devrenno.bookland.catalog.domain.entity.Category;
import com.devrenno.bookland.catalog.domain.valueobject.BookId;
import com.devrenno.bookland.catalog.domain.valueobject.CategoryId;
import com.devrenno.bookland.catalog.domain.valueobject.ISBN;
import com.devrenno.bookland.catalog.domain.valueobject.Price;
import com.devrenno.bookland.catalog.infrastructure.persistence.entity.BookJpaEntity;
import com.devrenno.bookland.catalog.infrastructure.persistence.entity.CategoryJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CatalogPersistenceMapper {

    default Book toDomain(BookJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Book.reconstitute(
                BookId.of(entity.getId()),
                entity.getTitle(),
                ISBN.of(entity.getIsbn()),
                entity.getAuthors(),
                entity.getPublisher(),
                entity.getPublicationYear(),
                entity.getEdition(),
                entity.getSynopsis(),
                Price.of(entity.getPrice()),
                entity.getStockQuantity(),
                CategoryId.of(entity.getCategory().getId()),
                entity.getAvgRating(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

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

    default Category toDomain(CategoryJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Category.of(CategoryId.of(entity.getId()), entity.getName(), entity.isActive());
    }
}
