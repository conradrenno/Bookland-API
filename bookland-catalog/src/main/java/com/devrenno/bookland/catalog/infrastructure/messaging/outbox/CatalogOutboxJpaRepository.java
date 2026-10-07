package com.devrenno.bookland.catalog.infrastructure.messaging.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CatalogOutboxJpaRepository extends JpaRepository<CatalogOutboxJpaEntity, UUID> {

    /** The relay's batch: pending messages, oldest first, so one order's replies leave in the order they were written. */
    List<CatalogOutboxJpaEntity> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
}
