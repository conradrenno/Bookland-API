package com.devrenno.bookland.catalog.application.dto;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        long bookCount
) {}
