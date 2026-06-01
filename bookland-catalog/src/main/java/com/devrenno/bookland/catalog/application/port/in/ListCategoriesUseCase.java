package com.devrenno.bookland.catalog.application.port.in;

import com.devrenno.bookland.catalog.application.dto.CategoryResponse;

import java.util.List;

public interface ListCategoriesUseCase {
    List<CategoryResponse> execute();
}
