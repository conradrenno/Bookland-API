package com.devrenno.bookland.reviews.infrastructure.messaging.outbox;

import com.devrenno.bookland.reviews.application.dto.BookRatingChanged;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Pins the producer's half of the contract — the JSON field names the catalog reads — and what the
 * relay needs from the row: the key, the type it picks the topic from, and a pending state.
 */
@ExtendWith(MockitoExtension.class)
class OutboxBookRatingEventAdapterTest {

    @Mock private ReviewOutboxJpaRepository repository;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private final BookRatingChanged event = new BookRatingChanged(
            UUID.randomUUID(), UUID.randomUUID(), 4.5, 2, Instant.parse("2026-10-05T12:00:00Z"));

    @Test
    void publish_shouldStoreThePendingEventAsJson() {
        new OutboxBookRatingEventAdapter(repository, jsonMapper).publish(event);

        ArgumentCaptor<ReviewOutboxJpaEntity> stored = ArgumentCaptor.forClass(ReviewOutboxJpaEntity.class);
        verify(repository).save(stored.capture());
        ReviewOutboxJpaEntity row = stored.getValue();
        assertThat(row.getId()).isEqualTo(event.eventId());
        assertThat(row.getAggregateId()).isEqualTo(event.bookId());
        assertThat(row.getEventType()).isEqualTo("BookRatingChanged");
        assertThat(row.getCreatedAt()).isEqualTo(event.occurredAt());
        assertThat(row.getPublishedAt()).isNull();

        JsonNode json = jsonMapper.readTree(row.getPayload());
        assertThat(json.get("eventId").asString()).isEqualTo(event.eventId().toString());
        assertThat(json.get("bookId").asString()).isEqualTo(event.bookId().toString());
        assertThat(json.get("averageRating").asDouble()).isEqualTo(4.5);
        assertThat(json.get("reviewCount").asInt()).isEqualTo(2);
        assertThat(json.get("occurredAt").asString()).isEqualTo("2026-10-05T12:00:00Z");
    }
}
