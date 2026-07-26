package com.devrenno.bookland.catalog.domain.valueobject;

import java.util.Objects;

/**
 * ISBN-13 in canonical form: 13 digits, no separators. Hyphens and spaces are accepted on input
 * (they are pure presentation) and stripped, so "978-0132350884" and "9780132350884" are the same
 * ISBN — which is what keeps uniqueness checks, stored values and API responses consistent.
 */
public record ISBN(String value) {

    public ISBN {
        Objects.requireNonNull(value, "ISBN cannot be null");
        value = value.replaceAll("[\\s-]", "");
        if (!value.matches("\\d{13}")) {
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
