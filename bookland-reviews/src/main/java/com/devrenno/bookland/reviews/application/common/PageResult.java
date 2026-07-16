package com.devrenno.bookland.reviews.application.common;

import java.util.List;
import java.util.function.Function;

/**
 * Framework-free paginated result. Replaces Spring's Page in the inner layers.
 */
public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(content.stream().map(mapper).toList(), page, size, totalElements, totalPages);
    }
}
