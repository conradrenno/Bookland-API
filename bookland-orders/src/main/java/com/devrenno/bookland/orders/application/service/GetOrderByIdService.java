package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.annotation.UseCase;
import com.devrenno.bookland.orders.application.dto.OrderResponse;
import com.devrenno.bookland.orders.application.port.in.GetOrderByIdUseCase;
import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.domain.exception.OrderAccessDeniedException;
import com.devrenno.bookland.orders.domain.exception.OrderNotFoundException;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class GetOrderByIdService implements GetOrderByIdUseCase {

    private final OrderPersistencePort orderPersistencePort;

    @Override
    public OrderResponse execute(UUID orderId, UUID requesterId, boolean isAdmin) {
        Order order = orderPersistencePort.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!isAdmin && !order.getCustomerId().equals(requesterId)) {
            throw new OrderAccessDeniedException(orderId);
        }
        return OrderResponseMapper.toOrderResponse(order);
    }
}
