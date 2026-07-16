package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.dto.UpdateOrderStatusCommand;
import com.devrenno.bookland.orders.application.port.in.UpdateOrderStatusUseCase;
import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.domain.exception.OrderNotFoundException;

public class UpdateOrderStatusService implements UpdateOrderStatusUseCase {

    private final OrderPersistencePort orderPersistencePort;

    private UpdateOrderStatusService(OrderPersistencePort orderPersistencePort) {
        this.orderPersistencePort = orderPersistencePort;
    }

    public static UpdateOrderStatusService create(OrderPersistencePort orderPersistencePort) {
        return new UpdateOrderStatusService(orderPersistencePort);
    }

    @Override
    public Order execute(UpdateOrderStatusCommand command) {
        Order order = orderPersistencePort.findById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));
        order.transitionStatus(command.newStatus(), command.adminId());
        return orderPersistencePort.save(order);
    }
}
