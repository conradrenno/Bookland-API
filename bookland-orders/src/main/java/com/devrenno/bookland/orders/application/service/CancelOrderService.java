package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.port.in.CancelOrderUseCase;
import com.devrenno.bookland.orders.application.port.out.OrderEventPort;
import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.application.port.out.TransactionPort;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.domain.entity.OrderStatus;
import com.devrenno.bookland.orders.domain.exception.OrderNotFoundException;

import java.util.UUID;

public class CancelOrderService implements CancelOrderUseCase {

    private final OrderPersistencePort orderPersistencePort;
    private final OrderEventPort orderEventPort;
    private final TransactionPort transactionPort;

    private CancelOrderService(OrderPersistencePort orderPersistencePort, OrderEventPort orderEventPort,
                               TransactionPort transactionPort) {
        this.orderPersistencePort = orderPersistencePort;
        this.orderEventPort = orderEventPort;
        this.transactionPort = transactionPort;
    }

    public static CancelOrderService create(OrderPersistencePort orderPersistencePort, OrderEventPort orderEventPort,
                                            TransactionPort transactionPort) {
        return new CancelOrderService(orderPersistencePort, orderEventPort, transactionPort);
    }

    @Override
    public Order execute(UUID orderId, UUID customerId) {
        return transactionPort.inTransaction(() -> {
            Order order = orderPersistencePort.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException(orderId));

            OrderStatus previousStatus = order.getStatus();
            order.cancel(customerId);

            OrderEvents.announce(order, previousStatus, orderEventPort);

            return orderPersistencePort.save(order);
        });
    }
}
