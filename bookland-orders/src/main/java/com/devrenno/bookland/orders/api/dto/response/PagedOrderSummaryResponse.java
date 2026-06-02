package com.devrenno.bookland.orders.api.dto.response;

import com.devrenno.bookland.orders.application.dto.OrderSummaryResponse;

import java.util.List;

public record PagedOrderSummaryResponse(
        List<OrderSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
