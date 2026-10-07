package com.devrenno.bookland.catalog.infrastructure.messaging;

import com.devrenno.bookland.catalog.application.dto.StockReservationResult;
import com.devrenno.bookland.catalog.application.port.in.ReleaseStockForOrderUseCase;
import com.devrenno.bookland.catalog.application.port.in.ReserveStockForOrderUseCase;
import com.devrenno.bookland.catalog.domain.entity.StockReservation;
import com.devrenno.bookland.catalog.infrastructure.messaging.StockMessages.StockCommand;
import com.devrenno.bookland.catalog.infrastructure.messaging.StockMessages.StockReply;
import com.devrenno.bookland.catalog.infrastructure.messaging.inbox.CatalogInbox;
import com.devrenno.bookland.catalog.infrastructure.messaging.outbox.CatalogOutbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

/**
 * Inbound adapter for the stock commands of the checkout saga — the messaging counterpart of a
 * {@code @RestController}: it turns a command into a use-case call and the result into a reply.
 *
 * <p>Each command is handled in one transaction holding three things together: the inbox record (so
 * a redelivery is skipped), the stock change, and the reply in the outbox. Either all three commit or
 * none does, and the redelivery then does the work for real.
 *
 * <p>The use cases are the same ones the synchronous checkout called; only the way the request
 * arrives changed. They are idempotent by order id on their own, so the inbox is a second line —
 * it also keeps a duplicate {@code ReserveStock} from producing a second reply.
 */
@Component
public class StockCommandListener {

    private static final Logger log = LoggerFactory.getLogger(StockCommandListener.class);

    private final ReserveStockForOrderUseCase reserveStockForOrderUseCase;
    private final ReleaseStockForOrderUseCase releaseStockForOrderUseCase;
    private final CatalogInbox inbox;
    private final CatalogOutbox outbox;
    private final TransactionTemplate transactionTemplate;
    private final JsonMapper jsonMapper;

    public StockCommandListener(ReserveStockForOrderUseCase reserveStockForOrderUseCase,
                                ReleaseStockForOrderUseCase releaseStockForOrderUseCase,
                                CatalogInbox inbox, CatalogOutbox outbox,
                                PlatformTransactionManager transactionManager, JsonMapper jsonMapper) {
        this.reserveStockForOrderUseCase = reserveStockForOrderUseCase;
        this.releaseStockForOrderUseCase = releaseStockForOrderUseCase;
        this.inbox = inbox;
        this.outbox = outbox;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = CatalogKafkaConfig.STOCK_COMMANDS_TOPIC, groupId = "bookland-catalog",
            containerFactory = CatalogKafkaConfig.LISTENER_CONTAINER_FACTORY)
    public void on(String payload) {
        StockCommand command = jsonMapper.readValue(payload, StockCommand.class);
        transactionTemplate.executeWithoutResult(tx -> {
            if (!inbox.firstDelivery(command.messageId())) {
                log.info("Stock command {} for order {} already handled, skipped", command.type(), command.orderId());
                return;
            }
            switch (command.type()) {
                case CatalogKafkaConfig.RESERVE_STOCK -> reserve(command);
                case CatalogKafkaConfig.RELEASE_STOCK -> releaseStockForOrderUseCase.release(command.orderId());
                default -> log.warn("Unknown stock command type {}, ignored", command.type());
            }
        });
    }

    private void reserve(StockCommand command) {
        StockReservationResult result = reserveStockForOrderUseCase.reserve(command.orderId(), command.items().stream()
                .map(line -> new StockReservation.Item(line.bookId(), line.quantity()))
                .toList());
        String type = result.reserved() ? CatalogKafkaConfig.STOCK_RESERVED : CatalogKafkaConfig.STOCK_RESERVATION_FAILED;
        UUID messageId = UUID.randomUUID();
        outbox.append(messageId, command.orderId(), type,
                new StockReply(messageId, type, command.orderId(), result.unavailableBookIds()));
    }
}
