package com.devrenno.bookland.inventory.application.port.out;

import com.devrenno.bookland.inventory.domain.entity.InventoryEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface InventoryPersistencePort {
    InventoryEntry save(InventoryEntry entry);
    Page<InventoryEntry> findByBookId(UUID bookId, Pageable pageable);
    Optional<LocalDateTime> findLastAdjustmentTime(UUID bookId);
}
