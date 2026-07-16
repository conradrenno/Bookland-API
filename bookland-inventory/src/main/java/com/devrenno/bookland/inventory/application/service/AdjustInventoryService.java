package com.devrenno.bookland.inventory.application.service;

import com.devrenno.bookland.inventory.application.dto.AdjustInventoryCommand;
import com.devrenno.bookland.inventory.application.port.in.AdjustInventoryUseCase;
import com.devrenno.bookland.inventory.application.port.out.BookStockAdjustmentPort;
import com.devrenno.bookland.inventory.application.port.out.InventoryPersistencePort;
import com.devrenno.bookland.inventory.domain.entity.InventoryEntry;

public class AdjustInventoryService implements AdjustInventoryUseCase {

    private final BookStockAdjustmentPort bookStockAdjustmentPort;
    private final InventoryPersistencePort inventoryPersistencePort;

    private AdjustInventoryService(BookStockAdjustmentPort bookStockAdjustmentPort,
                                  InventoryPersistencePort inventoryPersistencePort) {
        this.bookStockAdjustmentPort = bookStockAdjustmentPort;
        this.inventoryPersistencePort = inventoryPersistencePort;
    }

    public static AdjustInventoryService create(BookStockAdjustmentPort bookStockAdjustmentPort,
                                                InventoryPersistencePort inventoryPersistencePort) {
        return new AdjustInventoryService(bookStockAdjustmentPort, inventoryPersistencePort);
    }

    @Override
    public InventoryEntry execute(AdjustInventoryCommand command) {
        int previousQty = bookStockAdjustmentPort.getCurrentStock(command.bookId());
        int newQty = bookStockAdjustmentPort.adjustStock(command.bookId(), command.delta());

        InventoryEntry entry = InventoryEntry.create(
                command.bookId(), previousQty, newQty, command.reason(), command.adjustedBy()
        );
        return inventoryPersistencePort.save(entry);
    }
}
