package com.devrenno.bookland.catalog.api.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BookApiResponse(
        UUID id,
        String title,
        String isbn,
        List<String> authors,
        String publisher,
        Integer publicationYear,
        String edition,
        String synopsis,
        BigDecimal price,
        int stockQuantity,
        boolean available,
        UUID categoryId,
        double avgRating
) {}
