package com.devrenno.bookland.notification.infrastructure.messaging.inbox;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * The order events already handled. Kafka delivers at least once — the orders relay resends a row
 * it sent but could not stamp — so an event can arrive twice; the second finds its id here and does
 * not queue a second email.
 *
 * <p>Recorded in the caller's transaction ({@code MANDATORY}): if queueing the task fails, the
 * record rolls back with it and the redelivery is handled for real.
 */
@Component
public class NotificationInbox {

    private final NotificationInboxJpaRepository repository;

    public NotificationInbox(NotificationInboxJpaRepository repository) {
        this.repository = repository;
    }

    /** True the first time a message id is seen; false for a redelivery, which the caller must ignore. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean firstDelivery(UUID messageId) {
        if (repository.existsById(messageId)) {
            return false;
        }
        repository.save(new NotificationInboxJpaEntity(messageId, Instant.now()));
        return true;
    }
}
