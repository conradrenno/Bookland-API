package com.devrenno.bookland.catalog.application.dto;

import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public record BookSearchQuery(
        String q,
        UUID categoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Pageable pageable
) {}
