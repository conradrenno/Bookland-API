package com.devrenno.bookland.payments.infrastructure.messaging;

import com.devrenno.bookland.payments.application.port.in.RefundPaymentUseCase;
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
 * {@code OrderCancelled} and expects no answer, so a refund that fails for good is a problem for
 * payments to solve, not for the order — it is logged and skipped by the container's error handler.
 *
 * <p>Two guards against refunding twice, as with the charge: the inbox skips a message seen before,
 * and {@code RefundPaymentService} leaves a payment already REFUNDED alone.
 */
@Component("paymentsOrderCancelledListener")
public class OrderCancelledListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCancelledListener.class);

    private final RefundPaymentUseCase refundPaymentUseCase;
    private final PaymentsInbox inbox;
    private final TransactionTemplate transactionTemplate;
    private final JsonMapper jsonMapper;

    public OrderCancelledListener(RefundPaymentUseCase refundPaymentUseCase, PaymentsInbox inbox,
                                  PlatformTransactionManager transactionManager, JsonMapper jsonMapper) {
        this.refundPaymentUseCase = refundPaymentUseCase;
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
            refundPaymentUseCase.refund(event.orderId());
        });
    }
}
