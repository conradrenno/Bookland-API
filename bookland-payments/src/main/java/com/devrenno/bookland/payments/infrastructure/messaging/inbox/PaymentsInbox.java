package com.devrenno.bookland.payments.infrastructure.messaging.inbox;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * The payments module's record of messages already consumed. Kafka delivers at least once — a relay that
 * crashes between sending and stamping sends again — so a message can arrive twice; the second
 * finds its id here and is skipped.
 *
 * <p>Recorded in the caller's transaction ({@code MANDATORY}), together with the effect: if the
 * effect rolls back, so does the record, and the redelivery is processed for real.
 */
@Component
public class PaymentsInbox {

    private final PaymentsInboxJpaRepository repository;

    public PaymentsInbox(PaymentsInboxJpaRepository repository) {
        this.repository = repository;
    }

    /** True the first time a message id is seen; false for a redelivery, which the caller must ignore. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean firstDelivery(UUID messageId) {
        if (repository.existsById(messageId)) {
            return false;
        }
        repository.save(new PaymentsInboxJpaEntity(messageId, Instant.now()));
        return true;
    }
}
