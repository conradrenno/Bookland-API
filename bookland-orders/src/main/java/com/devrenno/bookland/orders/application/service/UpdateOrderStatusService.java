package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.annotation.UseCase;
import com.devrenno.bookland.orders.application.dto.OrderResponse;
import com.devrenno.bookland.orders.application.dto.UpdateOrderStatusCommand;
import com.devrenno.bookland.orders.application.port.in.UpdateOrderStatusUseCase;
import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.domain.exception.OrderNotFoundException;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdateOrderStatusService implements UpdateOrderStatusUseCase {

    private final OrderPersistencePort orderPersistencePort;

    @Override
    public OrderResponse execute(UpdateOrderStatusCommand command) {
        Order order = orderPersistencePort.findById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));
        order.transitionStatus(command.newStatus(), command.adminId());
        return OrderResponseMapper.toOrderResponse(orderPersistencePort.save(order));
    }
}
