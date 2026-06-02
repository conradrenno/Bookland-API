package com.devrenno.bookland.inventory.application.port.in;

import com.devrenno.bookland.inventory.application.dto.InventoryEntryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface GetInventoryHistoryUseCase {
    Page<InventoryEntryResponse> execute(UUID bookId, Pageable pageable);
}
