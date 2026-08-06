package com.devrenno.bookland.inventory.application.port.out;

import java.util.UUID;

public interface BookStockAdjustmentPort {

    /**
     * Applies a signed correction and returns the resulting stock. There is no companion read of the
     * current stock on purpose: the caller derives the previous quantity from this result, because
     * any separate read would be a different snapshot than the one the adjustment applied to.
     */
    int adjustStock(UUID bookId, int delta);
}
