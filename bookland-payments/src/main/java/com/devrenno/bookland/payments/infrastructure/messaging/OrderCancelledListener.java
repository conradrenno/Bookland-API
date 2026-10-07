package com.devrenno.bookland.payments.infrastructure.messaging;

import com.devrenno.bookland.payments.application.port.in.RequestRefundUseCase;
import com.devrenno.bookland.payments.infrastructure.messaging.inbox.PaymentsInbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * Refunds a cancelled order. The payments half of the choreographed cancellation: orders announces
 * {@code OrderCancelled} and expects no answer, so a refund that goes wrong is a problem for payments
 * to solve, not for the order.
 *
 * <p>Like the charge, it only records the intention (REFUND_PENDING) with the inbox record; the
 * gateway worker makes the refund, retrying while the gateway does not answer, and marks it
 * REFUND_FAILED if the gateway refuses for good. An event that cannot be applied at all (no payment
 * for the order, a payment that never took money) goes to this module's dead-letter topic.
 *
 * <p>Two guards against refunding twice: the inbox skips a message seen before, and a payment whose
 * refund was already requested is left alone. A third sits at the gateway: the idempotency key.
 */
@Component("paymentsOrderCancelledListener")
public class OrderCancelledListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCancelledListener.class);

    private final RequestRefundUseCase requestRefundUseCase;
    private final PaymentsInbox inbox;
    private final TransactionTemplate transactionTemplate;
    private final JsonMapper jsonMapper;

    public OrderCancelledListener(RequestRefundUseCase requestRefundUseCase, PaymentsInbox inbox,
                                  PlatformTransactionManager transactionManager, JsonMapper jsonMapper) {
        this.requestRefundUseCase = requestRefundUseCase;
        this.inbox = inbox;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = PaymentsKafkaConfig.ORDER_EVENTS_TOPIC, groupId = "bookland-payments",
            containerFactory = PaymentsKafkaConfig.LISTENER_CONTAINER_FACTORY)
    public void on(String payload) {
        OrderEventMessage event = jsonMapper.readValue(payload, OrderEventMessage.class);
        if (!PaymentsKafkaConfig.ORDER_CANCELLED.equals(event.type())) {
            return;
        }
        transactionTemplate.executeWithoutResult(tx -> {
            if (!inbox.firstDelivery(event.messageId())) {
                log.info("OrderCancelled for order {} already handled, skipped", event.orderId());
                return;
            }
            requestRefundUseCase.requestRefund(event.orderId());
        });
    }
}
