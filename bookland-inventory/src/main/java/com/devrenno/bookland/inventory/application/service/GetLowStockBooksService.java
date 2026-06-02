package com.devrenno.bookland.inventory.application.service;

import com.devrenno.bookland.inventory.application.annotation.UseCase;
import com.devrenno.bookland.inventory.application.dto.LowStockBookResponse;
import com.devrenno.bookland.inventory.application.port.in.GetLowStockBooksUseCase;
import com.devrenno.bookland.inventory.application.port.out.InventoryPersistencePort;
import com.devrenno.bookland.inventory.application.port.out.LowStockBooksPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

@UseCase
@RequiredArgsConstructor
public class GetLowStockBooksService implements GetLowStockBooksUseCase {

    private final LowStockBooksPort lowStockBooksPort;
    private final InventoryPersistencePort inventoryPersistencePort;

    @Override
    public Page<LowStockBookResponse> execute(int threshold, Pageable pageable) {
        return lowStockBooksPort.getLowStockBooks(threshold, pageable)
                .map(info -> {
                    Optional<LocalDateTime> lastMovement =
                            inventoryPersistencePort.findLastAdjustmentTime(info.id());
                    return new LowStockBookResponse(
                            info.id(), info.title(), info.isbn(),
                            info.stockQuantity(), lastMovement.orElse(null)
                    );
                });
    }
}
