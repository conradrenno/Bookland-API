package com.devrenno.bookland.catalog.infrastructure.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpdateBookRequest(

        String title,

        List<@NotBlank String> authors,

        String publisher,

        @Min(1000)
        @Max(2100)
        Integer publicationYear,

        String edition,

        String synopsis,

        @DecimalMin(value = "0.01", message = "must be greater than 0")
        BigDecimal price,

        @Min(0)
        Integer stockQuantity,

        UUID categoryId,

        @Size(max = 2048)
        String coverImageUrl
) {}
