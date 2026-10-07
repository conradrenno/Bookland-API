package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.dto.UpdateOrderStatusCommand;
import com.devrenno.bookland.orders.application.port.in.UpdateOrderStatusUseCase;
import com.devrenno.bookland.orders.application.port.out.OrderEventPort;
import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.application.port.out.TransactionPort;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.domain.entity.OrderStatus;
import com.devrenno.bookland.orders.domain.exception.OrderNotFoundException;

public class UpdateOrderStatusService implements UpdateOrderStatusUseCase {

    private final OrderPersistencePort orderPersistencePort;
    private final OrderEventPort orderEventPort;
    private final TransactionPort transactionPort;

    private UpdateOrderStatusService(OrderPersistencePort orderPersistencePort, OrderEventPort orderEventPort,
                                     TransactionPort transactionPort) {
        this.orderPersistencePort = orderPersistencePort;
        this.orderEventPort = orderEventPort;
        this.transactionPort = transactionPort;
    }

    public static UpdateOrderStatusService create(OrderPersistencePort orderPersistencePort,
                                                  OrderEventPort orderEventPort, TransactionPort transactionPort) {
        return new UpdateOrderStatusService(orderPersistencePort, orderEventPort, transactionPort);
    }

    /**
     * The admin back-office drives the same state machine the customer does, and CONFIRMED → CANCELLED
     * is one of its legal moves — so this path owes the same compensation the customer's cancellation
     * does. It runs inside a transaction because a cancellation is two writes, the order and the
     * {@code OrderCancelled} event in the outbox: the event must not leave for an order still
     * CONFIRMED, nor the order be cancelled with no event to give its stock and money back.
     */
    @Override
    public Order execute(UpdateOrderStatusCommand command) {
        return transactionPort.inTransaction(() -> {
            Order order = orderPersistencePort.findById(command.orderId())
                    .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

            OrderStatus previousStatus = order.getStatus();
            order.transitionStatus(command.newStatus(), command.adminId());

            OrderCancellation.announce(order, previousStatus, command.newStatus(), orderEventPort);

            return orderPersistencePort.save(order);
        });
    }
}
