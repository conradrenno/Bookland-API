package com.devrenno.bookland.inventory.application.service;

import com.devrenno.bookland.inventory.application.annotation.UseCase;
import com.devrenno.bookland.inventory.application.dto.AdjustInventoryCommand;
import com.devrenno.bookland.inventory.application.dto.InventoryEntryResponse;
import com.devrenno.bookland.inventory.application.port.in.AdjustInventoryUseCase;
import com.devrenno.bookland.inventory.application.port.out.BookStockAdjustmentPort;
import com.devrenno.bookland.inventory.application.port.out.InventoryPersistencePort;
import com.devrenno.bookland.inventory.domain.entity.InventoryEntry;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class AdjustInventoryService implements AdjustInventoryUseCase {

    private final BookStockAdjustmentPort bookStockAdjustmentPort;
    private final InventoryPersistencePort inventoryPersistencePort;

    @Override
    public InventoryEntryResponse execute(AdjustInventoryCommand command) {
        int previousQty = bookStockAdjustmentPort.getCurrentStock(command.bookId());
        int newQty = bookStockAdjustmentPort.adjustStock(command.bookId(), command.delta());

        InventoryEntry entry = InventoryEntry.create(
                command.bookId(), previousQty, newQty, command.reason(), command.adjustedBy()
        );
        InventoryEntry saved = inventoryPersistencePort.save(entry);

        return new InventoryEntryResponse(
                saved.getId(), saved.getBookId(),
                saved.getPreviousQuantity(), saved.getNewQuantity(), saved.getDelta(),
                saved.getReason(), saved.getAdjustedBy(), saved.getAdjustedAt()
        );
    }
}
