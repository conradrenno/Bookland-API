package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.port.out.OrderEventPort;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.domain.entity.OrderStatus;

/**
 * What a cancellation owes the rest of the system — the stock back to the catalog, the money back to
 * the customer — announced as one {@code OrderCancelled} event. The catalog and payments each act on
 * it by themselves; orders no longer calls either of them.
 *
 * <p>This lives apart from any single service because two paths cancel an order — the customer's
 * {@link CancelOrderService} and the admin's {@link UpdateOrderStatusService} — and both owe the same
 * debt. Keeping the effect next to only one of them is what once let an admin cancellation leave the
 * customer charged and the stock short.
 *
 * <p>Only a cancellation coming out of CONFIRMED is announced: that is the state in which the stock
 * was reserved and the payment approved. Cancelling is not allowed earlier, while the checkout is
 * still running, and any other transition out of CONFIRMED (to SHIPPED) is not a cancellation at all
 * — hence both statuses are part of the decision, and the decision lives here so no caller can get it
 * half right.
 */
final class OrderCancellation {

    private OrderCancellation() {}

    static void announce(Order order, OrderStatus previousStatus, OrderStatus newStatus,
                         OrderEventPort orderEventPort) {
        if (newStatus != OrderStatus.CANCELLED || previousStatus != OrderStatus.CONFIRMED) {
            return;
        }
        orderEventPort.orderCancelled(order.getId());
    }
}
