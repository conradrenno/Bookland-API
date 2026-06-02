package com.devrenno.bookland.inventory.application.port.out;

import java.util.UUID;

public interface BookStockAdjustmentPort {
    int getCurrentStock(UUID bookId);
    int adjustStock(UUID bookId, int delta);
}
