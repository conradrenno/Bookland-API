package com.devrenno.bookland.catalog.infrastructure.messaging;

import com.devrenno.bookland.catalog.application.port.in.UpdateBookAverageRatingUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** Pins the consumer's half of the contract: the JSON the reviews module publishes. */
@ExtendWith(MockitoExtension.class)
class BookRatingChangedListenerTest {

    @Mock private UpdateBookAverageRatingUseCase updateBookAverageRatingUseCase;

    private BookRatingChangedListener listener;

    @BeforeEach
    void setUp() {
        listener = new BookRatingChangedListener(updateBookAverageRatingUseCase, JsonMapper.builder().build());
    }

    @Test
    void on_shouldUpdateTheBooksRating() {
        UUID bookId = UUID.randomUUID();

        listener.on("""
                {"eventId": "%s", "bookId": "%s", "averageRating": 4.5, "reviewCount": 2,
                 "occurredAt": "2026-10-05T12:00:00Z"}
                """.formatted(UUID.randomUUID(), bookId));

        verify(updateBookAverageRatingUseCase).updateAverageRating(bookId, 4.5);
    }

    /** A field the catalog does not know about must not break it — the producer may add one. */
    @Test
    void on_shouldIgnoreFieldsItDoesNotKnow() {
        UUID bookId = UUID.randomUUID();

        listener.on("""
                {"eventId": "%s", "bookId": "%s", "averageRating": 3.0, "someNewField": true}
                """.formatted(UUID.randomUUID(), bookId));

        verify(updateBookAverageRatingUseCase).updateAverageRating(bookId, 3.0);
    }

    /** Thrown as a JacksonException, which the container's error handler skips without retrying. */
    @Test
    void on_shouldRejectAPayloadThatIsNotJson() {
        assertThatThrownBy(() -> listener.on("not json")).isInstanceOf(JacksonException.class);

        verifyNoInteractions(updateBookAverageRatingUseCase);
    }
}
