package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.dto.CategoryWithCount;
import com.devrenno.bookland.catalog.application.port.in.ListCategoriesUseCase;
import com.devrenno.bookland.catalog.application.port.out.CategoryPersistencePort;

import java.util.List;

public class ListCategoriesService implements ListCategoriesUseCase {

    private final CategoryPersistencePort categoryPersistencePort;

    private ListCategoriesService(CategoryPersistencePort categoryPersistencePort) {
        this.categoryPersistencePort = categoryPersistencePort;
    }

    public static ListCategoriesService create(CategoryPersistencePort categoryPersistencePort) {
        return new ListCategoriesService(categoryPersistencePort);
    }

    @Override
    public List<CategoryWithCount> execute() {
        return categoryPersistencePort.findAllActiveWithBookCount();
    }
}
