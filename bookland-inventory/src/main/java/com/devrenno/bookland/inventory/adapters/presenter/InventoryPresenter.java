package com.devrenno.bookland.inventory.adapters.presenter;

import com.devrenno.bookland.inventory.adapters.viewmodel.InventoryEntryViewModel;
import com.devrenno.bookland.inventory.adapters.viewmodel.LowStockBookViewModel;
import com.devrenno.bookland.inventory.application.dto.LowStockBook;
import com.devrenno.bookland.inventory.domain.entity.InventoryEntry;

/**
 * Transforms domain entities / query read-models into delivery-facing view models. Plain Java.
 */
public class InventoryPresenter {

    private InventoryPresenter() {
    }

    public static InventoryPresenter create() {
        return new InventoryPresenter();
    }

    public InventoryEntryViewModel present(InventoryEntry entry) {
        return new InventoryEntryViewModel(
                entry.getId(),
                entry.getBookId(),
                entry.getPreviousQuantity(),
                entry.getNewQuantity(),
                entry.getDelta(),
                entry.getReason(),
                entry.getAdjustedBy(),
                entry.getAdjustedAt()
        );
    }

    public LowStockBookViewModel present(LowStockBook book) {
        return new LowStockBookViewModel(
                book.id(),
                book.title(),
                book.isbn(),
                book.stockQuantity(),
                book.lastMovement()
        );
    }
}
