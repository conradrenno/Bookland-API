package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.dto.UpdateOrderStatusCommand;
import com.devrenno.bookland.orders.application.port.in.UpdateOrderStatusUseCase;
import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.application.port.out.RefundPort;
import com.devrenno.bookland.orders.application.port.out.StockReservationPort;
import com.devrenno.bookland.orders.application.port.out.TransactionPort;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.domain.entity.OrderStatus;
import com.devrenno.bookland.orders.domain.exception.OrderNotFoundException;

public class UpdateOrderStatusService implements UpdateOrderStatusUseCase {

    private final OrderPersistencePort orderPersistencePort;
    private final StockReservationPort stockReservationPort;
    private final RefundPort refundPort;
    private final TransactionPort transactionPort;

    private UpdateOrderStatusService(OrderPersistencePort orderPersistencePort, StockReservationPort stockReservationPort,
                                     RefundPort refundPort, TransactionPort transactionPort) {
        this.orderPersistencePort = orderPersistencePort;
        this.stockReservationPort = stockReservationPort;
        this.refundPort = refundPort;
        this.transactionPort = transactionPort;
    }

    public static UpdateOrderStatusService create(OrderPersistencePort orderPersistencePort,
                                                  StockReservationPort stockReservationPort, RefundPort refundPort,
                                                  TransactionPort transactionPort) {
        return new UpdateOrderStatusService(orderPersistencePort, stockReservationPort, refundPort, transactionPort);
    }

    /**
     * The admin back-office drives the same state machine the customer does, and CONFIRMED → CANCELLED
     * is one of its legal moves — so this path owes the same compensation the customer's cancellation
     * does. It runs inside a transaction because a cancellation is three writes (stock, refund, order):
     * a failure after the first two would otherwise leave the money returned on an order still
     * CONFIRMED.
     */
    @Override
    public Order execute(UpdateOrderStatusCommand command) {
        return transactionPort.inTransaction(() -> {
            Order order = orderPersistencePort.findById(command.orderId())
                    .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

            OrderStatus previousStatus = order.getStatus();
            order.transitionStatus(command.newStatus(), command.adminId());

            OrderCancellation.compensate(order, previousStatus, command.newStatus(),
                    stockReservationPort, refundPort);

            return orderPersistencePort.save(order);
        });
    }
}
