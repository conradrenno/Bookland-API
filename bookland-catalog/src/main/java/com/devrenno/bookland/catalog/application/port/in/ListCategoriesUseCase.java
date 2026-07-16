package com.devrenno.bookland.catalog.application.port.in;

import com.devrenno.bookland.catalog.application.dto.CategoryWithCount;

import java.util.List;

public interface ListCategoriesUseCase {
    List<CategoryWithCount> execute();
}
