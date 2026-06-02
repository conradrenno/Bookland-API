package com.devrenno.bookland.orders.application.port.in;

import com.devrenno.bookland.orders.application.dto.OrderSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface GetOrderHistoryUseCase {
    Page<OrderSummaryResponse> execute(UUID customerId, Pageable pageable);
}
