package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.port.in.CancelOrderUseCase;
import com.devrenno.bookland.orders.application.port.out.BookStockPort;
import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.application.port.out.RefundPort;
import com.devrenno.bookland.orders.application.port.out.TransactionPort;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.domain.entity.OrderStatus;
import com.devrenno.bookland.orders.domain.exception.OrderNotFoundException;

import java.util.UUID;

public class CancelOrderService implements CancelOrderUseCase {

    private final OrderPersistencePort orderPersistencePort;
    private final BookStockPort bookStockPort;
    private final RefundPort refundPort;
    private final TransactionPort transactionPort;

    private CancelOrderService(OrderPersistencePort orderPersistencePort, BookStockPort bookStockPort,
                               RefundPort refundPort, TransactionPort transactionPort) {
        this.orderPersistencePort = orderPersistencePort;
        this.bookStockPort = bookStockPort;
        this.refundPort = refundPort;
        this.transactionPort = transactionPort;
    }

    public static CancelOrderService create(OrderPersistencePort orderPersistencePort,
                                            BookStockPort bookStockPort, RefundPort refundPort,
                                            TransactionPort transactionPort) {
        return new CancelOrderService(orderPersistencePort, bookStockPort, refundPort, transactionPort);
    }

    @Override
    public Order execute(UUID orderId, UUID customerId) {
        return transactionPort.inTransaction(() -> {
            Order order = orderPersistencePort.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException(orderId));

            OrderStatus previousStatus = order.getStatus();
            order.cancel(customerId);

            if (previousStatus == OrderStatus.CONFIRMED) {
                for (var item : order.getItems()) {
                    bookStockPort.adjustStock(item.getBookId(), item.getQuantity());
                }
                refundPort.refund(orderId);
            }

            return orderPersistencePort.save(order);
        });
    }
}
