package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.port.out.BookStockPort;
import com.devrenno.bookland.orders.application.port.out.RefundPort;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.domain.entity.OrderStatus;

/**
 * The side effects a cancellation owes the rest of the system: the stock the order took goes back to
 * the catalog and the money the customer paid goes back to the customer.
 *
 * <p>This lives apart from any single service because two paths cancel an order — the customer's
 * {@link CancelOrderService} and the admin's {@link UpdateOrderStatusService} — and both owe the same
 * debt. Keeping the effect next to only one of them is what once let an admin cancellation leave the
 * customer charged and the stock short.
 *
 * <p>Only a cancellation coming out of CONFIRMED compensates: that is the state in which the stock was
 * decremented and the payment approved (see CheckoutService). An order cancelled out of
 * AWAITING_PAYMENT never took either, and any other transition out of CONFIRMED (to SHIPPED) is not a
 * cancellation at all — hence both statuses are part of the decision, and the decision lives here so
 * no caller can get it half right.
 */
final class OrderCancellation {

    private OrderCancellation() {}

    static void compensate(Order order, OrderStatus previousStatus, OrderStatus newStatus,
                           BookStockPort bookStockPort, RefundPort refundPort) {
        if (newStatus != OrderStatus.CANCELLED || previousStatus != OrderStatus.CONFIRMED) {
            return;
        }
        for (var item : order.getItems()) {
            bookStockPort.incrementStock(item.getBookId(), item.getQuantity());
        }
        refundPort.refund(order.getId());
    }
}
