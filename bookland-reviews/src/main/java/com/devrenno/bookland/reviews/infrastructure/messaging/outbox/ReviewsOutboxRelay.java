package com.devrenno.bookland.reviews.infrastructure.messaging.outbox;

import com.devrenno.bookland.reviews.infrastructure.messaging.ReviewsKafkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Moves the outbox to Kafka: every second, the pending rows, oldest first, each one sent and waited
 * for before it is stamped as published.
 *
 * <p>Three rules carry the correctness:
 * <ul>
 *   <li><b>At least once.</b> A crash after the broker accepted a row and before it was stamped sends
 *       that row again on the next round. Consumers must tolerate a duplicate — the rating event does,
 *       since it carries the resulting rating, not a delta.</li>
 *   <li><b>Stop at the first failure.</b> Skipping a row that failed and sending the next could deliver
 *       a book's older rating after its newer one.</li>
 *   <li><b>Wait for the broker's answer.</b> {@code get()} has no timeout of its own because the
 *       producer bounds it: the future completes, sent or failed, within {@code delivery.timeout.ms}
 *       (two minutes by default). Giving up earlier would leave the record in the producer's buffer
 *       and send the row again on the next round, piling duplicates up while the broker is down.</li>
 * </ul>
 *
 * <p><b>One instance only.</b> Two application instances would read the same pending rows and both
 * send them. Before the reviews service runs replicated, the read must claim its rows
 * ({@code SELECT ... FOR UPDATE SKIP LOCKED}) or the relay must run on an elected leader.
 */
@Component
public class ReviewsOutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(ReviewsOutboxRelay.class);

    private static final Map<String, String> TOPICS = Map.of(
            OutboxBookRatingEventAdapter.EVENT_TYPE, ReviewsKafkaConfig.BOOK_RATING_CHANGED_TOPIC);

    private final ReviewOutboxJpaRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public ReviewsOutboxRelay(ReviewOutboxJpaRepository repository, KafkaTemplate<String, String> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 1000)
    public void relay() {
        for (ReviewOutboxJpaEntity row : repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            try {
                kafkaTemplate.send(topicOf(row), row.getAggregateId().toString(), row.getPayload()).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException | RuntimeException e) {
                log.warn("Outbox event {} ({}) not published yet, will retry: {}",
                        row.getId(), row.getEventType(), e.getMessage());
                return;
            }
            row.setPublishedAt(Instant.now());
            repository.save(row);
        }
    }

    private String topicOf(ReviewOutboxJpaEntity row) {
        String topic = TOPICS.get(row.getEventType());
        if (topic == null) {
            throw new IllegalStateException("No topic for outbox event type " + row.getEventType());
        }
        return topic;
    }
}
