package com.devrenno.bookland.user.domain.valueobject;

import java.util.UUID;

public record UserId(UUID value) {

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId of(UUID id) {
        return new UserId(id);
    }

    public static UserId of(String id) {
        return new UserId(UUID.fromString(id));
    }
}
