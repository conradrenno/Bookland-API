package com.devrenno.bookland.inventory.application.service;

import com.devrenno.bookland.inventory.application.dto.AdjustInventoryCommand;
import com.devrenno.bookland.inventory.application.port.in.AdjustInventoryUseCase;
import com.devrenno.bookland.inventory.application.port.out.BookStockAdjustmentPort;
import com.devrenno.bookland.inventory.application.port.out.InventoryPersistencePort;
import com.devrenno.bookland.inventory.application.port.out.TransactionPort;
import com.devrenno.bookland.inventory.domain.entity.InventoryEntry;

public class AdjustInventoryService implements AdjustInventoryUseCase {

    private final BookStockAdjustmentPort bookStockAdjustmentPort;
    private final InventoryPersistencePort inventoryPersistencePort;
    private final TransactionPort transactionPort;

    private AdjustInventoryService(BookStockAdjustmentPort bookStockAdjustmentPort,
                                   InventoryPersistencePort inventoryPersistencePort,
                                   TransactionPort transactionPort) {
        this.bookStockAdjustmentPort = bookStockAdjustmentPort;
        this.inventoryPersistencePort = inventoryPersistencePort;
        this.transactionPort = transactionPort;
    }

    public static AdjustInventoryService create(BookStockAdjustmentPort bookStockAdjustmentPort,
                                                InventoryPersistencePort inventoryPersistencePort,
                                                TransactionPort transactionPort) {
        return new AdjustInventoryService(bookStockAdjustmentPort, inventoryPersistencePort,
                transactionPort);
    }

    /**
     * The adjustment and its ledger entry commit together, and {@code previousQuantity} is derived
     * from the quantity the adjustment actually produced rather than read beforehand.
     *
     * <p>Both parts fix the same defect. Reading the stock first and adjusting second left a window
     * in which another writer could move the book, so the ledger recorded a "previous" that was
     * never the value this adjustment started from — the audit trail disagreed with the stock it was
     * supposed to explain. Without a transaction, a failure to save the entry left the stock already
     * changed and unrecorded. Deriving {@code previous} by subtracting the delta from the result is
     * exact because the adjustment applied that delta atomically, and the row lock it took is still
     * held here.</p>
     */
    @Override
    public InventoryEntry execute(AdjustInventoryCommand command) {
        return transactionPort.inTransaction(() -> {
            int newQty = bookStockAdjustmentPort.adjustStock(command.bookId(), command.delta());
            int previousQty = newQty - command.delta();

            InventoryEntry entry = InventoryEntry.create(
                    command.bookId(), previousQty, newQty, command.reason(), command.adjustedBy()
            );
            return inventoryPersistencePort.save(entry);
        });
    }
}
