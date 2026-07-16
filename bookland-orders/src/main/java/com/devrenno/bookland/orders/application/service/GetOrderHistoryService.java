package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.common.PageQuery;
import com.devrenno.bookland.orders.application.common.PageResult;
import com.devrenno.bookland.orders.application.port.in.GetOrderHistoryUseCase;
import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.domain.entity.Order;

import java.util.UUID;

public class GetOrderHistoryService implements GetOrderHistoryUseCase {

    private final OrderPersistencePort orderPersistencePort;

    private GetOrderHistoryService(OrderPersistencePort orderPersistencePort) {
        this.orderPersistencePort = orderPersistencePort;
    }

    public static GetOrderHistoryService create(OrderPersistencePort orderPersistencePort) {
        return new GetOrderHistoryService(orderPersistencePort);
    }

    @Override
    public PageResult<Order> execute(UUID customerId, PageQuery pageQuery) {
        return orderPersistencePort.findByCustomerId(customerId, pageQuery);
    }
}
