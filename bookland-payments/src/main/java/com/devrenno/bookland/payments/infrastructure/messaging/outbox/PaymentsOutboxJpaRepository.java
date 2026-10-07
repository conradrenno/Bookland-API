package com.devrenno.bookland.payments.infrastructure.messaging.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentsOutboxJpaRepository extends JpaRepository<PaymentsOutboxJpaEntity, UUID> {

    /** The relay's batch: pending messages, oldest first, so one order's messages leave in the order they were written. */
    List<PaymentsOutboxJpaEntity> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
}
