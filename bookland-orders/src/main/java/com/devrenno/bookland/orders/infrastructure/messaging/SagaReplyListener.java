package com.devrenno.bookland.orders.infrastructure.messaging;

import com.devrenno.bookland.orders.application.port.in.CheckoutSagaUseCase;
import com.devrenno.bookland.orders.infrastructure.messaging.SagaMessages.PaymentReply;
import com.devrenno.bookland.orders.infrastructure.messaging.SagaMessages.StockReply;
import com.devrenno.bookland.orders.infrastructure.messaging.inbox.OrdersInbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Inbound adapter for the replies of the checkout saga. Each reply is handled in one transaction that
 * holds the inbox record, the order's new status and the next command in the outbox.
 */
@Component
public class SagaReplyListener {

    private static final Logger log = LoggerFactory.getLogger(SagaReplyListener.class);

    private final CheckoutSagaUseCase checkoutSaga;
    private final OrdersInbox inbox;
    private final TransactionTemplate transactionTemplate;
    private final JsonMapper jsonMapper;

    public SagaReplyListener(CheckoutSagaUseCase checkoutSaga, OrdersInbox inbox,
                             PlatformTransactionManager transactionManager, JsonMapper jsonMapper) {
        this.checkoutSaga = checkoutSaga;
        this.inbox = inbox;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = OrdersKafkaConfig.STOCK_REPLIES_TOPIC, groupId = "bookland-orders",
            containerFactory = OrdersKafkaConfig.LISTENER_CONTAINER_FACTORY)
    public void onStockReply(String payload) {
        StockReply reply = jsonMapper.readValue(payload, StockReply.class);
        BooleanSupplier step = switch (reply.type()) {
            case OrdersKafkaConfig.STOCK_RESERVED -> () -> checkoutSaga.onStockReserved(reply.orderId());
            case OrdersKafkaConfig.STOCK_RESERVATION_FAILED -> () -> checkoutSaga.onStockReservationFailed(
                    reply.orderId(), reply.unavailableBookIds() == null ? List.of() : reply.unavailableBookIds());
            default -> null;
        };
        handle(reply.messageId(), reply.type(), reply.orderId(), step);
    }

    @KafkaListener(topics = OrdersKafkaConfig.PAYMENT_REPLIES_TOPIC, groupId = "bookland-orders",
            containerFactory = OrdersKafkaConfig.LISTENER_CONTAINER_FACTORY)
    public void onPaymentReply(String payload) {
        PaymentReply reply = jsonMapper.readValue(payload, PaymentReply.class);
        BooleanSupplier step = switch (reply.type()) {
            case OrdersKafkaConfig.PAYMENT_APPROVED -> () -> checkoutSaga.onPaymentApproved(reply.orderId());
            case OrdersKafkaConfig.PAYMENT_DECLINED -> () -> checkoutSaga.onPaymentDeclined(reply.orderId(), reply.reason());
            default -> null;
        };
        handle(reply.messageId(), reply.type(), reply.orderId(), step);
    }

    private void handle(UUID messageId, String type, UUID orderId, BooleanSupplier step) {
        if (step == null) {
            log.warn("Unknown saga reply type {}, ignored", type);
            return;
        }
        transactionTemplate.executeWithoutResult(tx -> {
            if (!inbox.firstDelivery(messageId)) {
                log.info("{} for order {} already handled, skipped", type, orderId);
            } else if (!step.getAsBoolean()) {
                log.info("{} for order {} does not match its current status: duplicate or late, ignored",
                        type, orderId);
            }
        });
    }
}
