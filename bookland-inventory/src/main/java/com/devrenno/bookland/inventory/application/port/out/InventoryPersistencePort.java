package com.devrenno.bookland.inventory.application.port.out;

import com.devrenno.bookland.inventory.application.common.PageQuery;
import com.devrenno.bookland.inventory.application.common.PageResult;
import com.devrenno.bookland.inventory.domain.entity.InventoryEntry;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface InventoryPersistencePort {
    InventoryEntry save(InventoryEntry entry);
    PageResult<InventoryEntry> findByBookId(UUID bookId, PageQuery pageQuery);
    Optional<LocalDateTime> findLastAdjustmentTime(UUID bookId);
}
