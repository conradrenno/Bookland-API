package com.devrenno.bookland.catalog.infrastructure.messaging;

import com.devrenno.bookland.catalog.application.port.in.UpdateBookAverageRatingUseCase;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Inbound adapter for the reviews module's rating events — the messaging counterpart of a
 * {@code @RestController}: it turns "a message arrived" into a use-case call.
 *
 * <p>The event carries the resulting average, not a delta, so a redelivered event writes the same
 * rating again and needs no deduplication here.
 */
@Component
public class BookRatingChangedListener {

    /** The topic name is the contract with the producer; it is duplicated on purpose, not imported from reviews. */
    static final String TOPIC = "bookland.reviews.book-rating-changed";

    private final UpdateBookAverageRatingUseCase updateBookAverageRatingUseCase;
    private final JsonMapper jsonMapper;

    public BookRatingChangedListener(UpdateBookAverageRatingUseCase updateBookAverageRatingUseCase,
                                     JsonMapper jsonMapper) {
        this.updateBookAverageRatingUseCase = updateBookAverageRatingUseCase;
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = TOPIC, groupId = "bookland-catalog",
            containerFactory = CatalogKafkaConfig.LISTENER_CONTAINER_FACTORY)
    public void on(String payload) {
        BookRatingChangedMessage message = jsonMapper.readValue(payload, BookRatingChangedMessage.class);
        updateBookAverageRatingUseCase.updateAverageRating(message.bookId(), message.averageRating());
    }
}
