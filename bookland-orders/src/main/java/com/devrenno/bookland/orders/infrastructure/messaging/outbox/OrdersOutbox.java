package com.devrenno.bookland.orders.infrastructure.messaging.outbox;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

/**
 * Writes a message to the orders module's outbox, in the caller's transaction — the same one that changed
 * its data — so the change and the message announcing it commit together or not at all.
 * {@link OrdersOutboxRelay} sends it to Kafka afterwards.
 *
 * <p>{@code MANDATORY}: called outside a transaction it fails instead of committing the row alone.
 */
@Component
public class OrdersOutbox {

    private final OrdersOutboxJpaRepository repository;
    private final JsonMapper jsonMapper;

    public OrdersOutbox(OrdersOutboxJpaRepository repository, JsonMapper jsonMapper) {
        this.repository = repository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void append(UUID messageId, UUID aggregateId, String eventType, Object payload) {
        repository.save(new OrdersOutboxJpaEntity(messageId, aggregateId, eventType,
                jsonMapper.writeValueAsString(payload), Instant.now(), null));
    }
}
