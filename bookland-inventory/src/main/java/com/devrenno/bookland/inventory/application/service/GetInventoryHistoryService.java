package com.devrenno.bookland.inventory.application.service;

import com.devrenno.bookland.inventory.application.common.PageQuery;
import com.devrenno.bookland.inventory.application.common.PageResult;
import com.devrenno.bookland.inventory.application.port.in.GetInventoryHistoryUseCase;
import com.devrenno.bookland.inventory.application.port.out.InventoryPersistencePort;
import com.devrenno.bookland.inventory.domain.entity.InventoryEntry;

import java.util.UUID;

public class GetInventoryHistoryService implements GetInventoryHistoryUseCase {

    private final InventoryPersistencePort inventoryPersistencePort;

    private GetInventoryHistoryService(InventoryPersistencePort inventoryPersistencePort) {
        this.inventoryPersistencePort = inventoryPersistencePort;
    }

    public static GetInventoryHistoryService create(InventoryPersistencePort inventoryPersistencePort) {
        return new GetInventoryHistoryService(inventoryPersistencePort);
    }

    @Override
    public PageResult<InventoryEntry> execute(UUID bookId, PageQuery pageQuery) {
        return inventoryPersistencePort.findByBookId(bookId, pageQuery);
    }
}
