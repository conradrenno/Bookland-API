package com.devrenno.bookland.catalog.domain.valueobject;

import java.util.UUID;

public record BookId(UUID value) {

    public static BookId generate() {
        return new BookId(UUID.randomUUID());
    }

    public static BookId of(UUID id) {
        return new BookId(id);
    }

    public static BookId of(String id) {
        return new BookId(UUID.fromString(id));
    }
}
