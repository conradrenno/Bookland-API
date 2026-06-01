package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.annotation.UseCase;
import com.devrenno.bookland.catalog.application.dto.CategoryResponse;
import com.devrenno.bookland.catalog.application.port.in.ListCategoriesUseCase;
import com.devrenno.bookland.catalog.application.port.out.CategoryPersistencePort;
import lombok.RequiredArgsConstructor;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class ListCategoriesService implements ListCategoriesUseCase {

    private final CategoryPersistencePort categoryPersistencePort;

    @Override
    public List<CategoryResponse> execute() {
        return categoryPersistencePort.findAllActiveWithBookCount();
    }
}
