package com.devrenno.bookland.inventory.application.port.in;

import com.devrenno.bookland.inventory.application.dto.AdjustInventoryCommand;
import com.devrenno.bookland.inventory.application.dto.InventoryEntryResponse;

public interface AdjustInventoryUseCase {
    InventoryEntryResponse execute(AdjustInventoryCommand command);
}
