package com.devrenno.bookland.orders.infrastructure.messaging;

import com.devrenno.bookland.orders.application.port.out.OrderEventPort;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.infrastructure.messaging.SagaMessages.OrderEvent;
import com.devrenno.bookland.orders.infrastructure.messaging.SagaMessages.OrderEventItem;
import com.devrenno.bookland.orders.infrastructure.messaging.outbox.OrdersOutbox;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Announces an order's events by writing them to the orders outbox, in the transaction that changed
 * the order. The relay publishes them to {@link OrdersKafkaConfig#ORDER_EVENTS_TOPIC}, keyed by the
 * order id, so one order's events reach every consumer in the order they happened.
 */
@Component
public class OutboxOrderEventAdapter implements OrderEventPort {

    private final OrdersOutbox outbox;

    public OutboxOrderEventAdapter(OrdersOutbox outbox) {
        this.outbox = outbox;
    }

    @Override
    public void orderConfirmed(Order order) {
        append(OrdersKafkaConfig.ORDER_CONFIRMED, order);
    }

    @Override
    public void orderPaymentFailed(Order order) {
        append(OrdersKafkaConfig.ORDER_PAYMENT_FAILED, order);
    }

    @Override
    public void orderRejected(Order order) {
        append(OrdersKafkaConfig.ORDER_REJECTED, order);
    }

    @Override
    public void orderShipped(Order order) {
        append(OrdersKafkaConfig.ORDER_SHIPPED, order);
    }

    @Override
    public void orderCancelled(Order order) {
        append(OrdersKafkaConfig.ORDER_CANCELLED, order);
    }

    private void append(String type, Order order) {
        UUID messageId = UUID.randomUUID();
        outbox.append(messageId, order.getId(), type, new OrderEvent(messageId, type, order.getId(),
                order.getCustomerId(), order.getCustomerEmail(), order.getCustomerName(),
                order.getStatus().name(), order.getStatusReason(), order.getTotalAmount(),
                order.getItems().stream()
                        .map(item -> new OrderEventItem(item.getBookId(), item.getTitle(), item.getQuantity(),
                                item.getUnitPrice()))
                        .toList(),
                order.getUpdatedAt()));
    }
}
