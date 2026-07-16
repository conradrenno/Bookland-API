package com.devrenno.bookland.inventory.application.port.in;

import com.devrenno.bookland.inventory.application.common.PageQuery;
import com.devrenno.bookland.inventory.application.common.PageResult;
import com.devrenno.bookland.inventory.domain.entity.InventoryEntry;

import java.util.UUID;

public interface GetInventoryHistoryUseCase {
    PageResult<InventoryEntry> execute(UUID bookId, PageQuery pageQuery);
}
