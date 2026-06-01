package com.devrenno.bookland.catalog.domain.valueobject;

import java.math.BigDecimal;
import java.util.Objects;

public record Price(BigDecimal value) {

    public Price {
        Objects.requireNonNull(value, "Price cannot be null");
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive: " + value);
        }
    }

    public static Price of(BigDecimal value) {
        return new Price(value);
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
