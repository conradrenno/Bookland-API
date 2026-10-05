package com.devrenno.bookland.reviews.infrastructure.messaging.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewOutboxJpaRepository extends JpaRepository<ReviewOutboxJpaEntity, UUID> {

    /** The relay's batch: pending events, oldest first, so one book's events leave in the order they were written. */
    List<ReviewOutboxJpaEntity> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
}
