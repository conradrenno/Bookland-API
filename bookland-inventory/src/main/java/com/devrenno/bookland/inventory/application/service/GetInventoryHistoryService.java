package com.devrenno.bookland.inventory.application.service;

import com.devrenno.bookland.inventory.application.annotation.UseCase;
import com.devrenno.bookland.inventory.application.dto.InventoryEntryResponse;
import com.devrenno.bookland.inventory.application.port.in.GetInventoryHistoryUseCase;
import com.devrenno.bookland.inventory.application.port.out.InventoryPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class GetInventoryHistoryService implements GetInventoryHistoryUseCase {

    private final InventoryPersistencePort inventoryPersistencePort;

    @Override
    public Page<InventoryEntryResponse> execute(UUID bookId, Pageable pageable) {
        return inventoryPersistencePort.findByBookId(bookId, pageable)
                .map(e -> new InventoryEntryResponse(
                        e.getId(), e.getBookId(),
                        e.getPreviousQuantity(), e.getNewQuantity(), e.getDelta(),
                        e.getReason(), e.getAdjustedBy(), e.getAdjustedAt()
                ));
    }
}
