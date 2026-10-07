package com.devrenno.bookland.orders.infrastructure.messaging;

import com.devrenno.bookland.orders.application.port.out.OrderEventPort;
import com.devrenno.bookland.orders.infrastructure.messaging.SagaMessages.OrderCancelled;
import com.devrenno.bookland.orders.infrastructure.messaging.outbox.OrdersOutbox;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Announces an order's events by writing them to the orders outbox, in the transaction that changed
 * the order. The relay publishes them to {@link OrdersKafkaConfig#ORDER_EVENTS_TOPIC}, keyed by the
 * order id, where the catalog and payments each consume them under their own group.
 */
@Component
public class OutboxOrderEventAdapter implements OrderEventPort {

    private final OrdersOutbox outbox;

    public OutboxOrderEventAdapter(OrdersOutbox outbox) {
        this.outbox = outbox;
    }

    @Override
    public void orderCancelled(UUID orderId) {
        UUID messageId = UUID.randomUUID();
        outbox.append(messageId, orderId, OrdersKafkaConfig.ORDER_CANCELLED,
                new OrderCancelled(messageId, OrdersKafkaConfig.ORDER_CANCELLED, orderId));
    }
}
