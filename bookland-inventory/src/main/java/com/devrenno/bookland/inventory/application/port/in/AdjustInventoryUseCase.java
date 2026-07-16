package com.devrenno.bookland.inventory.application.port.in;

import com.devrenno.bookland.inventory.application.dto.AdjustInventoryCommand;
import com.devrenno.bookland.inventory.domain.entity.InventoryEntry;

public interface AdjustInventoryUseCase {
    InventoryEntry execute(AdjustInventoryCommand command);
}
