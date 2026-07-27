package com.devrenno.bookland.catalog.infrastructure.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateBookRequest(

        @NotBlank
        @Size(max = 255)
        String title,

        // The pattern puts no ceiling on the separators, so it alone would let an ISBN past the
        // column and turn a bad request into a 500.
        @NotBlank
        @Size(max = 255)
        @Pattern(regexp = "\\s*(?:\\d[\\s-]*){13}", message = "must be 13 digits (hyphens optional)")
        String isbn,

        @NotEmpty
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

        @NotNull
        @DecimalMin(value = "0.01", message = "must be greater than 0")
        BigDecimal price,

        @Min(0)
        int stockQuantity,

        @NotNull
        UUID categoryId,

        // 255, not 2048: books.cover_image_url and order_items.cover_image_url are both
        // varchar(255), so anything longer was accepted here only to fail at the database.
        @Size(max = 255)
        String coverImageUrl
) {}
