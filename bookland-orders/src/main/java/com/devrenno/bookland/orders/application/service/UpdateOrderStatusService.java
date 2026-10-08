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
     * does, and CONFIRMED → SHIPPED the same notice to the customer. It runs inside a transaction
     * because a transition is two writes, the order and its event in the outbox: the event must not
     * leave for a change that did not commit, nor the change commit with no event to announce it.
     */
    @Override
    public Order execute(UpdateOrderStatusCommand command) {
        return transactionPort.inTransaction(() -> {
            Order order = orderPersistencePort.findById(command.orderId())
                    .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

            OrderStatus previousStatus = order.getStatus();
            order.transitionStatus(command.newStatus(), command.adminId());

            OrderEvents.announce(order, previousStatus, orderEventPort);

            return orderPersistencePort.save(order);
        });
    }
}
