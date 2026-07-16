package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.port.in.CheckActiveOrdersUseCase;
import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.domain.entity.OrderStatus;

import java.util.UUID;

public class CheckActiveOrdersService implements CheckActiveOrdersUseCase {

    private final OrderPersistencePort orderPersistencePort;

    private CheckActiveOrdersService(OrderPersistencePort orderPersistencePort) {
        this.orderPersistencePort = orderPersistencePort;
    }

    public static CheckActiveOrdersService create(OrderPersistencePort orderPersistencePort) {
        return new CheckActiveOrdersService(orderPersistencePort);
    }

    @Override
    public boolean hasActiveOrdersForBook(UUID bookId) {
        return orderPersistencePort.existsOrderWithBookInStatuses(bookId, OrderStatus.activeStatuses());
    }
}
