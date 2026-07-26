package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.common.PageQuery;
import com.devrenno.bookland.orders.application.common.PageResult;
import com.devrenno.bookland.orders.application.port.in.ListAllOrdersUseCase;
import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.domain.entity.OrderStatus;

public class ListAllOrdersService implements ListAllOrdersUseCase {

    private final OrderPersistencePort orderPersistencePort;

    private ListAllOrdersService(OrderPersistencePort orderPersistencePort) {
        this.orderPersistencePort = orderPersistencePort;
    }

    public static ListAllOrdersService create(OrderPersistencePort orderPersistencePort) {
        return new ListAllOrdersService(orderPersistencePort);
    }

    @Override
    public PageResult<Order> execute(OrderStatus status, PageQuery pageQuery) {
        return orderPersistencePort.findAll(status, pageQuery);
    }
}
