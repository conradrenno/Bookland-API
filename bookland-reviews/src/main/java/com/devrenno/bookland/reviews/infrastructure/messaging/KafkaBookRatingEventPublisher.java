package com.devrenno.bookland.reviews.infrastructure.messaging;

import com.devrenno.bookland.reviews.application.dto.BookRatingChanged;
import com.devrenno.bookland.reviews.application.port.out.BookRatingEventPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Publishes the rating event straight to Kafka, after the review has been saved.
 *
 * <p><b>Known gap:</b> the review and the event are two writes to two systems with no transaction
 * spanning both. If the broker is down when the review is saved, the event is lost: it is logged,
 * not retried, and the catalog keeps the old rating until the book's next review. The exception is
 * swallowed on purpose — rethrowing would answer 500 for a review that is already saved, and the
 * client's retry would then get a 409.
 *
 * <p>The value is a JSON string written here rather than by Spring Kafka's JSON serializer, which
 * would put this class's name in a type header and tie every consumer to a package of this module.
 */
@Component
public class KafkaBookRatingEventPublisher implements BookRatingEventPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaBookRatingEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    public KafkaBookRatingEventPublisher(KafkaTemplate<String, String> kafkaTemplate, JsonMapper jsonMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void publish(BookRatingChanged event) {
        String key = event.bookId().toString();
        try {
            kafkaTemplate.send(ReviewsKafkaConfig.BOOK_RATING_CHANGED_TOPIC, key, jsonMapper.writeValueAsString(event))
                    .whenComplete((result, failure) -> {
                        if (failure != null) {
                            logLost(event, failure);
                        }
                    });
        } catch (RuntimeException failure) {
            logLost(event, failure);
        }
    }

    private void logLost(BookRatingChanged event, Throwable failure) {
        log.error("BookRatingChanged {} for book {} was not published and is lost",
                event.eventId(), event.bookId(), failure);
    }
}
