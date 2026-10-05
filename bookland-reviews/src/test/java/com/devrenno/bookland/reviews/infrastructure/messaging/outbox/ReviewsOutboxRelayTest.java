package com.devrenno.bookland.reviews.infrastructure.messaging.outbox;

import com.devrenno.bookland.reviews.infrastructure.messaging.ReviewsKafkaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewsOutboxRelayTest {

    @Mock private ReviewOutboxJpaRepository repository;
    @Mock private KafkaTemplate<String, String> kafkaTemplate;

    private ReviewsOutboxRelay relay;

    @BeforeEach
    void setUp() {
        relay = new ReviewsOutboxRelay(repository, kafkaTemplate);
    }

    @Test
    void relay_shouldSendEveryPendingRowInOrderAndStampIt() {
        ReviewOutboxJpaEntity first = pendingRow("{\"n\":1}");
        ReviewOutboxJpaEntity second = pendingRow("{\"n\":2}");
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(first, second));
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(sent());

        relay.relay();

        InOrder order = inOrder(kafkaTemplate);
        order.verify(kafkaTemplate).send(ReviewsKafkaConfig.BOOK_RATING_CHANGED_TOPIC,
                first.getAggregateId().toString(), "{\"n\":1}");
        order.verify(kafkaTemplate).send(ReviewsKafkaConfig.BOOK_RATING_CHANGED_TOPIC,
                second.getAggregateId().toString(), "{\"n\":2}");
        assertThat(first.getPublishedAt()).isNotNull();
        assertThat(second.getPublishedAt()).isNotNull();
        verify(repository).save(first);
        verify(repository).save(second);
    }

    /**
     * Sending the second row after the first failed could deliver a book's older rating after its
     * newer one; and the failed row must stay pending so the next round retries it.
     */
    @Test
    void relay_shouldStopAtTheFirstFailureAndLeaveTheRowPending() {
        ReviewOutboxJpaEntity failing = pendingRow("{\"n\":1}");
        ReviewOutboxJpaEntity next = pendingRow("{\"n\":2}");
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(failing, next));
        when(kafkaTemplate.send(anyString(), eq(failing.getAggregateId().toString()), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new KafkaException("broker down")));

        relay.relay();

        verify(kafkaTemplate, never()).send(anyString(), eq(next.getAggregateId().toString()), anyString());
        assertThat(failing.getPublishedAt()).isNull();
        verify(repository, never()).save(any());
    }

    /** send() itself throws when the producer cannot even fetch the broker's metadata. */
    @Test
    void relay_shouldLeaveTheRowPending_whenSendThrows() {
        ReviewOutboxJpaEntity row = pendingRow("{}");
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(row));
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenThrow(new KafkaException("no metadata"));

        relay.relay();

        assertThat(row.getPublishedAt()).isNull();
        verify(repository, never()).save(any());
    }

    private static ReviewOutboxJpaEntity pendingRow(String payload) {
        return new ReviewOutboxJpaEntity(UUID.randomUUID(), UUID.randomUUID(),
                OutboxBookRatingEventAdapter.EVENT_TYPE, payload, Instant.now(), null);
    }

    private static CompletableFuture<SendResult<String, String>> sent() {
        return CompletableFuture.completedFuture(null);
    }
}
