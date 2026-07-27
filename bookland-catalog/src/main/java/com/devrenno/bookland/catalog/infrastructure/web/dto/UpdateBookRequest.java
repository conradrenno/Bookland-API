package com.devrenno.bookland.catalog.infrastructure.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpdateBookRequest(

        @Size(max = 255)
        String title,

        List<@NotBlank @Size(max = 255) String> authors,

        @Size(max = 255)
        String publisher,

        @Min(1000)
        @Max(2100)
        Integer publicationYear,

        @Size(max = 255)
        String edition,

        // No bound: books.synopsis is text, not varchar(255).
        String synopsis,

        @DecimalMin(value = "0.01", message = "must be greater than 0")
        BigDecimal price,

        @Min(0)
        Integer stockQuantity,

        UUID categoryId,

        // 255, not 2048: books.cover_image_url and order_items.cover_image_url are both
        // varchar(255), so anything longer was accepted here only to fail at the database.
        @Size(max = 255)
        String coverImageUrl
) {}
