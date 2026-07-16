package com.devrenno.bookland.inventory.application.common;

/**
 * Framework-free pagination request (zero-based page). Replaces Spring's Pageable in the inner layers.
 * Sorting is a fixed concern of each query and is applied by the persistence adapter, so it is not
 * carried here.
 */
public record PageQuery(int page, int size) {

    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be at least 1");
        }
    }

    public static PageQuery of(int page, int size) {
        return new PageQuery(page, size);
    }
}
