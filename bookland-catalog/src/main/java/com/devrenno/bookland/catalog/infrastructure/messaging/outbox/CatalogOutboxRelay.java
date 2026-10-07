package com.devrenno.bookland.catalog.infrastructure.messaging.outbox;

import com.devrenno.bookland.catalog.infrastructure.messaging.CatalogKafkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Moves the catalog's outbox to Kafka — the catalog's own copy of the relay the reviews module
 * introduced, so each module that leaves takes its relay with it. Same three rules: at least once,
 * stop at the first failure, wait for the broker's answer. Same limit: one instance only, until the
 * read claims its rows ({@code FOR UPDATE SKIP LOCKED}) or runs on an elected leader.
 */
@Component
public class CatalogOutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(CatalogOutboxRelay.class);

    private final CatalogOutboxJpaRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public CatalogOutboxRelay(CatalogOutboxJpaRepository repository, KafkaTemplate<String, String> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 1000)
    public void relay() {
        for (CatalogOutboxJpaEntity row : repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            try {
                kafkaTemplate.send(topicOf(row), row.getAggregateId().toString(), row.getPayload()).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException | RuntimeException e) {
                log.warn("Outbox message {} ({}) not published yet, will retry: {}",
                        row.getId(), row.getEventType(), e.getMessage());
                return;
            }
            row.setPublishedAt(Instant.now());
            repository.save(row);
        }
    }

    private static final Map<String, String> TOPICS = Map.of(
            CatalogKafkaConfig.STOCK_RESERVED, CatalogKafkaConfig.STOCK_REPLIES_TOPIC,
            CatalogKafkaConfig.STOCK_RESERVATION_FAILED, CatalogKafkaConfig.STOCK_REPLIES_TOPIC);

    private String topicOf(CatalogOutboxJpaEntity row) {
        String topic = TOPICS.get(row.getEventType());
        if (topic == null) {
            throw new IllegalStateException("No topic for outbox message type " + row.getEventType());
        }
        return topic;
    }
}
