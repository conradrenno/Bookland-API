package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.port.in.CancelOrderUseCase;
import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.application.port.out.RefundPort;
import com.devrenno.bookland.orders.application.port.out.StockReservationPort;
import com.devrenno.bookland.orders.application.port.out.TransactionPort;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.domain.entity.OrderStatus;
import com.devrenno.bookland.orders.domain.exception.OrderNotFoundException;

import java.util.UUID;

public class CancelOrderService implements CancelOrderUseCase {

    private final OrderPersistencePort orderPersistencePort;
    private final StockReservationPort stockReservationPort;
    private final RefundPort refundPort;
    private final TransactionPort transactionPort;

    private CancelOrderService(OrderPersistencePort orderPersistencePort, StockReservationPort stockReservationPort,
                               RefundPort refundPort, TransactionPort transactionPort) {
        this.orderPersistencePort = orderPersistencePort;
        this.stockReservationPort = stockReservationPort;
        this.refundPort = refundPort;
        this.transactionPort = transactionPort;
    }

    public static CancelOrderService create(OrderPersistencePort orderPersistencePort,
                                            StockReservationPort stockReservationPort, RefundPort refundPort,
                                            TransactionPort transactionPort) {
        return new CancelOrderService(orderPersistencePort, stockReservationPort, refundPort, transactionPort);
    }

    @Override
    public Order execute(UUID orderId, UUID customerId) {
        return transactionPort.inTransaction(() -> {
            Order order = orderPersistencePort.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException(orderId));

            OrderStatus previousStatus = order.getStatus();
            order.cancel(customerId);

            OrderCancellation.compensate(order, previousStatus, order.getStatus(),
                    stockReservationPort, refundPort);

            return orderPersistencePort.save(order);
        });
    }
}
