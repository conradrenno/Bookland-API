package com.devrenno.bookland.catalog.domain.entity;

import com.devrenno.bookland.catalog.domain.valueobject.CategoryId;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Category {

    private final CategoryId id;
    private final String name;
    private final boolean active;

    public static Category of(CategoryId id, String name, boolean active) {
        return Category.builder()
                .id(id)
                .name(name)
                .active(active)
                .build();
    }
}
