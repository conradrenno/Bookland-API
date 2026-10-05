package com.devrenno.bookland.reviews.infrastructure.messaging.outbox;

import com.devrenno.bookland.reviews.application.dto.BookRatingChanged;
import com.devrenno.bookland.reviews.application.port.out.BookRatingEventPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * "Publishing" a rating event means writing it to the outbox table, in the caller's transaction.
 * Kafka is not touched here: {@link ReviewsOutboxRelay} sends the row later. So the review and its
 * event commit together, and a broker that is down delays the event rather than losing it.
 *
 * <p>{@code MANDATORY} makes a call outside a transaction fail instead of committing the row on its
 * own — which would quietly bring back the two-separate-writes problem this class exists to remove.
 *
 * <p>The payload is serialized here, once: the relay sends exactly the stored string. The value is
 * a JSON string rather than Spring Kafka's JSON serializer, which would put this module's class name
 * in a type header and tie every consumer to it.
 */
@Component
public class OutboxBookRatingEventAdapter implements BookRatingEventPort {

    static final String EVENT_TYPE = "BookRatingChanged";

    private final ReviewOutboxJpaRepository repository;
    private final JsonMapper jsonMapper;

    public OutboxBookRatingEventAdapter(ReviewOutboxJpaRepository repository, JsonMapper jsonMapper) {
        this.repository = repository;
        this.jsonMapper = jsonMapper;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(BookRatingChanged event) {
        repository.save(new ReviewOutboxJpaEntity(event.eventId(), event.bookId(), EVENT_TYPE,
                jsonMapper.writeValueAsString(event), event.occurredAt(), null));
    }
}
