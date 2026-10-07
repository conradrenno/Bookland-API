package com.devrenno.bookland.payments.infrastructure.messaging.outbox;

import com.devrenno.bookland.payments.infrastructure.messaging.PaymentsKafkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Moves the payments module's outbox to Kafka — the payments module's own copy of the relay the reviews module
 * introduced, so each module that leaves takes its relay with it. Same three rules: at least once,
 * stop at the first failure, wait for the broker's answer. Same limit: one instance only, until the
 * read claims its rows ({@code FOR UPDATE SKIP LOCKED}) or runs on an elected leader.
 */
@Component
public class PaymentsOutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(PaymentsOutboxRelay.class);

    private final PaymentsOutboxJpaRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public PaymentsOutboxRelay(PaymentsOutboxJpaRepository repository, KafkaTemplate<String, String> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 1000)
    public void relay() {
        for (PaymentsOutboxJpaEntity row : repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()) {
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
            PaymentsKafkaConfig.PAYMENT_APPROVED, PaymentsKafkaConfig.PAYMENT_REPLIES_TOPIC,
            PaymentsKafkaConfig.PAYMENT_DECLINED, PaymentsKafkaConfig.PAYMENT_REPLIES_TOPIC);

    private String topicOf(PaymentsOutboxJpaEntity row) {
        String topic = TOPICS.get(row.getEventType());
        if (topic == null) {
            throw new IllegalStateException("No topic for outbox message type " + row.getEventType());
        }
        return topic;
    }
}
