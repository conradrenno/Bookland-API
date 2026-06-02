package com.devrenno.bookland.inventory.infrastructure.persistence.repository;

import com.devrenno.bookland.inventory.infrastructure.persistence.entity.InventoryEntryJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InventoryEntryJpaRepository extends JpaRepository<InventoryEntryJpaEntity, UUID> {

    Page<InventoryEntryJpaEntity> findByBookIdOrderByAdjustedAtDesc(UUID bookId, Pageable pageable);

    Optional<InventoryEntryJpaEntity> findFirstByBookIdOrderByAdjustedAtDesc(UUID bookId);
}
