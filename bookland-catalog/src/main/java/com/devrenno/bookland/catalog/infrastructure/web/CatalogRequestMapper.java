package com.devrenno.bookland.catalog.infrastructure.web;

import com.devrenno.bookland.catalog.application.dto.CreateBookCommand;
import com.devrenno.bookland.catalog.application.dto.UpdateBookCommand;
import com.devrenno.bookland.catalog.infrastructure.web.dto.CreateBookRequest;
import com.devrenno.bookland.catalog.infrastructure.web.dto.UpdateBookRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CatalogRequestMapper {

    CreateBookCommand toCommand(CreateBookRequest request);

    UpdateBookCommand toCommand(UpdateBookRequest request);
}
