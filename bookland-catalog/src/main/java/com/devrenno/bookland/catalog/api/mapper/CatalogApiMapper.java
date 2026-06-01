package com.devrenno.bookland.catalog.api.mapper;

import com.devrenno.bookland.catalog.api.dto.request.CreateBookRequest;
import com.devrenno.bookland.catalog.api.dto.request.UpdateBookRequest;
import com.devrenno.bookland.catalog.api.dto.response.BookApiResponse;
import com.devrenno.bookland.catalog.api.dto.response.CategoryApiResponse;
import com.devrenno.bookland.catalog.application.dto.CategoryResponse;
import com.devrenno.bookland.catalog.application.dto.CreateBookCommand;
import com.devrenno.bookland.catalog.application.dto.BookResponse;
import com.devrenno.bookland.catalog.application.dto.UpdateBookCommand;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CatalogApiMapper {

    CreateBookCommand toCommand(CreateBookRequest request);

    UpdateBookCommand toCommand(UpdateBookRequest request);

    BookApiResponse toApiResponse(BookResponse response);

    CategoryApiResponse toApiResponse(CategoryResponse response);
}
