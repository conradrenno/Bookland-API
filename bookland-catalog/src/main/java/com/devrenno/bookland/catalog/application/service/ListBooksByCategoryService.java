package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.annotation.UseCase;
import com.devrenno.bookland.catalog.application.dto.BookResponse;
import com.devrenno.bookland.catalog.application.mapper.CatalogApplicationMapper;
import com.devrenno.bookland.catalog.application.port.in.ListBooksByCategoryUseCase;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.application.port.out.CategoryPersistencePort;
import com.devrenno.bookland.catalog.domain.exception.CategoryNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class ListBooksByCategoryService implements ListBooksByCategoryUseCase {

    private final BookPersistencePort bookPersistencePort;
    private final CategoryPersistencePort categoryPersistencePort;
    private final CatalogApplicationMapper mapper;

    @Override
    public Page<BookResponse> execute(UUID categoryId, Pageable pageable) {
        categoryPersistencePort.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));

        return bookPersistencePort.findByCategoryId(categoryId, pageable).map(mapper::toResponse);
    }
}
