package com.devrenno.bookland.catalog.infrastructure.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateBookRequest(

        @NotBlank
        String title,

        @NotBlank
        @Pattern(regexp = "\\d{13}", message = "ISBN must be 13 digits")
        String isbn,

        @NotEmpty
        List<@NotBlank String> authors,

        String publisher,

        @Min(1000)
        @Max(2100)
        Integer publicationYear,

        String edition,

        String synopsis,

        @NotNull
        @DecimalMin(value = "0.01", message = "Price must be positive")
        BigDecimal price,

        @Min(0)
        int stockQuantity,

        @NotNull
        UUID categoryId
) {}
