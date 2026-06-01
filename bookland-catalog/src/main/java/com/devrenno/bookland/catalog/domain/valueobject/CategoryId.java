package com.devrenno.bookland.catalog.domain.valueobject;

import java.util.UUID;

public record CategoryId(UUID value) {

    public static CategoryId generate() {
        return new CategoryId(UUID.randomUUID());
    }

    public static CategoryId of(UUID id) {
        return new CategoryId(id);
    }

    public static CategoryId of(String id) {
        return new CategoryId(UUID.fromString(id));
    }
}
