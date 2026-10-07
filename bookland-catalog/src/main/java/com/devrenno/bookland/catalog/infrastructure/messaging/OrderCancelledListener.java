package com.devrenno.bookland.catalog.infrastructure.messaging;

import com.devrenno.bookland.catalog.application.port.in.ReleaseStockForOrderUseCase;
import com.devrenno.bookland.catalog.infrastructure.messaging.inbox.CatalogInbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * Gives a cancelled order's stock back. The catalog's half of the choreographed cancellation: orders
 * announces {@code OrderCancelled} and expects no answer, so there is no reply to write — only the
 * inbox record and the release, in one transaction.
 *
 * <p>The release is the same use case the saga's compensation calls, and it is idempotent by order
 * id on its own: only a reservation still RESERVED gives units back.
 */
@Component("catalogOrderCancelledListener")
public class OrderCancelledListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCancelledListener.class);

    private final ReleaseStockForOrderUseCase releaseStockForOrderUseCase;
    private final CatalogInbox inbox;
    private final TransactionTemplate transactionTemplate;
    private final JsonMapper jsonMapper;

    public OrderCancelledListener(ReleaseStockForOrderUseCase releaseStockForOrderUseCase, CatalogInbox inbox,
                                  PlatformTransactionManager transactionManager, JsonMapper jsonMapper) {
        this.releaseStockForOrderUseCase = releaseStockForOrderUseCase;
        this.inbox = inbox;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = CatalogKafkaConfig.ORDER_EVENTS_TOPIC, groupId = "bookland-catalog",
            containerFactory = CatalogKafkaConfig.LISTENER_CONTAINER_FACTORY)
    public void on(String payload) {
        OrderEventMessage event = jsonMapper.readValue(payload, OrderEventMessage.class);
        if (!CatalogKafkaConfig.ORDER_CANCELLED.equals(event.type())) {
            return;
        }
        transactionTemplate.executeWithoutResult(tx -> {
            if (!inbox.firstDelivery(event.messageId())) {
                log.info("OrderCancelled for order {} already handled, skipped", event.orderId());
                return;
            }
            releaseStockForOrderUseCase.release(event.orderId());
        });
    }
}
