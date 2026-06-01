package com.devrenno.bookland.catalog.application.port.out;

import com.devrenno.bookland.catalog.application.dto.CategoryResponse;
import com.devrenno.bookland.catalog.domain.entity.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryPersistencePort {
    Optional<Category> findById(UUID id);
    List<CategoryResponse> findAllActiveWithBookCount();
}
