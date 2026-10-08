package com.devrenno.bookland.notification.infrastructure.messaging;

import com.devrenno.bookland.notification.application.dto.NotifyOutcome;
import com.devrenno.bookland.notification.application.port.in.NotifyOrderEventUseCase;
import com.devrenno.bookland.notification.domain.valueobject.OrderEventKind;
import com.devrenno.bookland.notification.domain.valueobject.OrderLine;
import com.devrenno.bookland.notification.domain.valueobject.OrderNotice;
import com.devrenno.bookland.notification.infrastructure.messaging.inbox.NotificationInbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

/**
 * Reads the order events and turns the ones the customer is told about into email tasks. One
 * transaction holds the inbox record; the task is queued inside it and the queue's confirmation
 * awaited, so the event counts as handled — and its offset is committed — only once the task is
 * safely on the queue. If queueing fails, the inbox record rolls back and the event is retried.
 *
 * <p>The opposite crash — the task queued, then the process dies before the commit — delivers the
 * event again and queues a second task for the same email. The inbox cannot catch that one; the
 * email's key ({@code <orderId>:<KIND>}) is what lets the sender send it once (step 6c).
 */
@Component("notificationOrderEventListener")
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private static final Map<String, OrderEventKind> KINDS = Map.of(
            "OrderConfirmed", OrderEventKind.CONFIRMED,
            "OrderPaymentFailed", OrderEventKind.PAYMENT_FAILED,
            "OrderRejected", OrderEventKind.REJECTED,
            "OrderShipped", OrderEventKind.SHIPPED,
            "OrderCancelled", OrderEventKind.CANCELLED);

    private final NotifyOrderEventUseCase notifyOrderEventUseCase;
    private final NotificationInbox inbox;
    private final TransactionTemplate transactionTemplate;
    private final JsonMapper jsonMapper;

    public OrderEventListener(NotifyOrderEventUseCase notifyOrderEventUseCase, NotificationInbox inbox,
                              PlatformTransactionManager transactionManager, JsonMapper jsonMapper) {
        this.notifyOrderEventUseCase = notifyOrderEventUseCase;
        this.inbox = inbox;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = NotificationKafkaConfig.ORDER_EVENTS_TOPIC, groupId = NotificationKafkaConfig.CONSUMER_GROUP,
            containerFactory = NotificationKafkaConfig.LISTENER_CONTAINER_FACTORY)
    public void on(String payload) {
        OrderEventMessage event = jsonMapper.readValue(payload, OrderEventMessage.class);
        OrderEventKind kind = KINDS.get(event.type());
        if (kind == null) {
            return;
        }
        try {
            transactionTemplate.executeWithoutResult(tx -> {
                if (!inbox.firstDelivery(event.messageId())) {
                    log.info("{} for order {} already handled, skipped", event.type(), event.orderId());
                    return;
                }
                NotifyOutcome outcome = notifyOrderEventUseCase.notify(toNotice(event, kind));
                if (outcome == NotifyOutcome.NO_RECIPIENT) {
                    log.warn("{} for order {}: no email address on the order, nothing sent",
                            event.type(), event.orderId());
                }
            });
        } catch (RuntimeException e) {
            // The error handler retries without a word until it gives up: without this line, the
            // task queue being down would leave no trace in the log for minutes.
            log.warn("{} for order {} not queued yet, will retry: {}", event.type(), event.orderId(), e.getMessage());
            throw e;
        }
    }

    private static OrderNotice toNotice(OrderEventMessage event, OrderEventKind kind) {
        List<OrderLine> lines = event.items() == null ? List.of() : event.items().stream()
                .map(item -> new OrderLine(item.title(), item.quantity(), item.unitPrice()))
                .toList();
        return new OrderNotice(event.orderId(), kind, event.customerEmail(), event.customerName(),
                event.totalAmount(), lines, event.reason());
    }
}
