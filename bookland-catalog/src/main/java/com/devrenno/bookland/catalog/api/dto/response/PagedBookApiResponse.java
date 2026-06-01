package com.devrenno.bookland.catalog.api.dto.response;

import java.util.List;

public record PagedBookApiResponse(
        List<BookApiResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
