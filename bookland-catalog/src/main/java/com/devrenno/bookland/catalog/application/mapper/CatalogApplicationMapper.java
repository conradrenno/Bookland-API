package com.devrenno.bookland.catalog.application.mapper;

import com.devrenno.bookland.catalog.application.dto.BookResponse;
import com.devrenno.bookland.catalog.domain.entity.Book;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CatalogApplicationMapper {

    @Mapping(target = "id", expression = "java(book.getId().value())")
    @Mapping(target = "isbn", expression = "java(book.getIsbn().value())")
    @Mapping(target = "price", expression = "java(book.getPrice().value())")
    @Mapping(target = "categoryId", expression = "java(book.getCategoryId().value())")
    @Mapping(target = "available", expression = "java(book.getStockQuantity() > 0)")
    BookResponse toResponse(Book book);
}
