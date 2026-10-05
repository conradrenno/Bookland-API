package com.devrenno.bookland.reviews.infrastructure.messaging;

import com.devrenno.bookland.reviews.application.dto.BookRatingChanged;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pins the producer's half of the contract: topic, key, and the JSON field names the catalog reads. */
@ExtendWith(MockitoExtension.class)
class KafkaBookRatingEventPublisherTest {

    @Mock private KafkaTemplate<String, String> kafkaTemplate;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private KafkaBookRatingEventPublisher publisher;

    private final BookRatingChanged event = new BookRatingChanged(
            UUID.randomUUID(), UUID.randomUUID(), 4.5, 2, Instant.parse("2026-10-05T12:00:00Z"));

    @BeforeEach
    void setUp() {
        publisher = new KafkaBookRatingEventPublisher(kafkaTemplate, jsonMapper);
    }

    @Test
    void publish_shouldSendTheEventAsJsonKeyedByBook() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(new CompletableFuture<>());

        publisher.publish(event);

        ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(ReviewsKafkaConfig.BOOK_RATING_CHANGED_TOPIC),
                eq(event.bookId().toString()), value.capture());
        JsonNode json = jsonMapper.readTree(value.getValue());
        assertThat(json.get("eventId").asString()).isEqualTo(event.eventId().toString());
        assertThat(json.get("bookId").asString()).isEqualTo(event.bookId().toString());
        assertThat(json.get("averageRating").asDouble()).isEqualTo(4.5);
        assertThat(json.get("reviewCount").asInt()).isEqualTo(2);
        assertThat(json.get("occurredAt").asString()).isEqualTo("2026-10-05T12:00:00Z");
    }

    /** The review is already saved when the event goes out; a broker failure must not turn that into a 500. */
    @Test
    void publish_shouldNotThrow_whenTheBrokerIsUnavailable() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenThrow(new KafkaException("broker down"));

        assertThatCode(() -> publisher.publish(event)).doesNotThrowAnyException();
    }

    @Test
    void publish_shouldNotThrow_whenTheSendFailsAsynchronously() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.<SendResult<String, String>>failedFuture(new KafkaException("timeout")));

        assertThatCode(() -> publisher.publish(event)).doesNotThrowAnyException();
    }
}
