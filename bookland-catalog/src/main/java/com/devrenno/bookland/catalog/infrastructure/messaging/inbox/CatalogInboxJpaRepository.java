package com.devrenno.bookland.catalog.infrastructure.messaging.inbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CatalogInboxJpaRepository extends JpaRepository<CatalogInboxJpaEntity, UUID> {
}
