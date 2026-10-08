package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.port.out.OrderEventPort;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.domain.entity.OrderStatus;

/**
 * Which event a status change announces. Every path that moves an order — the saga's replies, the
 * customer's cancellation, the admin back-office — calls this after the transition, in the same
 * transaction, so no path can move an order without telling the rest of the system.
 *
 * <p>This lives apart from any single service because the same transition can come from more than
 * one path: CONFIRMED → CANCELLED is both the customer's {@link CancelOrderService} and the admin's
 * {@link UpdateOrderStatusService}, and both owe the stock and the money back. Keeping the
 * announcement next to only one of them is what once let an admin cancellation leave the customer
 * charged and the stock short.
 *
 * <p>The event follows from the status reached: the state machine allows only one way into each of
 * them (CANCELLED only out of CONFIRMED — the state in which the stock is reserved and the payment
 * approved). Steps nobody outside orders cares about (PENDING → AWAITING_PAYMENT) and DELIVERED
 * announce nothing.
 */
final class OrderEvents {

    private OrderEvents() {}

    static void announce(Order order, OrderStatus previousStatus, OrderEventPort orderEventPort) {
        if (order.getStatus() == previousStatus) {
            return;
        }
        switch (order.getStatus()) {
            case CONFIRMED -> orderEventPort.orderConfirmed(order);
            case PAYMENT_FAILED -> orderEventPort.orderPaymentFailed(order);
            case REJECTED -> orderEventPort.orderRejected(order);
            case SHIPPED -> orderEventPort.orderShipped(order);
            case CANCELLED -> orderEventPort.orderCancelled(order);
            default -> {
            }
        }
    }
}
