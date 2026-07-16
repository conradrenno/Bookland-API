package com.devrenno.bookland.catalog.domain.entity;

import com.devrenno.bookland.catalog.domain.valueobject.CategoryId;
import lombok.Getter;

@Getter
public class Category {

    private final CategoryId id;
    private final String name;
    private final boolean active;

    private Category(CategoryId id, String name, boolean active) {
        this.id = id;
        this.name = name;
        this.active = active;
    }

    public static Category of(CategoryId id, String name, boolean active) {
        return new Category(id, name, active);
    }
}
