package com.devrenno.bookland.catalog.domain.valueobject;

import java.util.Objects;

public record ISBN(String value) {

    public ISBN {
        Objects.requireNonNull(value, "ISBN cannot be null");
        String digits = value.replaceAll("-", "").trim();
        if (!digits.matches("\\d{13}")) {
            throw new IllegalArgumentException("ISBN must be 13 digits: " + value);
        }
    }

    public static ISBN of(String value) {
        return new ISBN(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
