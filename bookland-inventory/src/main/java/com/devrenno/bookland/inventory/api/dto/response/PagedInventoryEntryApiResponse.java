package com.devrenno.bookland.inventory.api.dto.response;

import java.util.List;

public record PagedInventoryEntryApiResponse(
        List<InventoryEntryApiResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
