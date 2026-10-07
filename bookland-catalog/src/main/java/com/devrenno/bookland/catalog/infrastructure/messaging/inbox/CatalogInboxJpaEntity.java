package com.devrenno.bookland.catalog.infrastructure.messaging.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** A message the catalog has already consumed. */
@Entity
@Table(name = "catalog_inbox")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CatalogInboxJpaEntity {

    @Id
    @Column(name = "message_id", nullable = false, updatable = false)
    private UUID messageId;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;
}
