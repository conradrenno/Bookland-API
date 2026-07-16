package com.devrenno.bookland.catalog.application.dto;

import com.devrenno.bookland.catalog.application.common.BookSort;

import java.math.BigDecimal;
import java.util.UUID;

public record BookSearchQuery(
        String q,
        UUID categoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        int page,
        int size,
        BookSort sort
) {}
