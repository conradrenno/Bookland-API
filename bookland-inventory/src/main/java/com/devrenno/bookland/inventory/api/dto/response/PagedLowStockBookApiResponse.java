package com.devrenno.bookland.inventory.api.dto.response;

import java.util.List;

public record PagedLowStockBookApiResponse(
        List<LowStockBookApiResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
