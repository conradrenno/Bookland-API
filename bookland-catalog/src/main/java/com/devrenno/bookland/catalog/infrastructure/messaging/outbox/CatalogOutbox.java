package com.devrenno.bookland.catalog.infrastructure.messaging.outbox;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

/**
 * Writes a message to the catalog's outbox, in the caller's transaction — the same one that changed
 * the stock — so the change and the message announcing it commit together or not at all.
 * {@link CatalogOutboxRelay} sends it to Kafka afterwards.
 *
 * <p>{@code MANDATORY}: called outside a transaction it fails instead of committing the row alone.
 */
@Component
public class CatalogOutbox {

    private final CatalogOutboxJpaRepository repository;
    private final JsonMapper jsonMapper;

    public CatalogOutbox(CatalogOutboxJpaRepository repository, JsonMapper jsonMapper) {
        this.repository = repository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void append(UUID messageId, UUID aggregateId, String eventType, Object payload) {
        repository.save(new CatalogOutboxJpaEntity(messageId, aggregateId, eventType,
                jsonMapper.writeValueAsString(payload), Instant.now(), null));
    }
}
