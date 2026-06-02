package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.annotation.UseCase;
import com.devrenno.bookland.orders.application.dto.OrderSummaryResponse;
import com.devrenno.bookland.orders.application.port.in.GetOrderHistoryUseCase;
import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class GetOrderHistoryService implements GetOrderHistoryUseCase {

    private final OrderPersistencePort orderPersistencePort;

    @Override
    public Page<OrderSummaryResponse> execute(UUID customerId, Pageable pageable) {
        return orderPersistencePort.findByCustomerId(customerId, pageable)
                .map(OrderResponseMapper::toSummaryResponse);
    }
}
