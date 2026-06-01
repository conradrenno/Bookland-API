package com.devrenno.bookland.catalog.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpdateBookCommand(
        String title,
        List<String> authors,
        String publisher,
        Integer publicationYear,
        String edition,
        String synopsis,
        BigDecimal price,
        Integer stockQuantity,
        UUID categoryId
) {}
