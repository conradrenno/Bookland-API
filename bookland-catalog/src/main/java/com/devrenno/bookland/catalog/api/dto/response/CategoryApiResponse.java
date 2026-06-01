package com.devrenno.bookland.catalog.api.dto.response;

import java.util.UUID;

public record CategoryApiResponse(
        UUID id,
        String name,
        long bookCount
) {}
