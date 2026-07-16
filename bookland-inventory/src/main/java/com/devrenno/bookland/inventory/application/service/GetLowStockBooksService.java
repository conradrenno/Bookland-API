package com.devrenno.bookland.inventory.application.service;

import com.devrenno.bookland.inventory.application.common.PageQuery;
import com.devrenno.bookland.inventory.application.common.PageResult;
import com.devrenno.bookland.inventory.application.dto.LowStockBook;
import com.devrenno.bookland.inventory.application.port.in.GetLowStockBooksUseCase;
import com.devrenno.bookland.inventory.application.port.out.InventoryPersistencePort;
import com.devrenno.bookland.inventory.application.port.out.LowStockBooksPort;

public class GetLowStockBooksService implements GetLowStockBooksUseCase {

    private final LowStockBooksPort lowStockBooksPort;
    private final InventoryPersistencePort inventoryPersistencePort;

    private GetLowStockBooksService(LowStockBooksPort lowStockBooksPort,
                                   InventoryPersistencePort inventoryPersistencePort) {
        this.lowStockBooksPort = lowStockBooksPort;
        this.inventoryPersistencePort = inventoryPersistencePort;
    }

    public static GetLowStockBooksService create(LowStockBooksPort lowStockBooksPort,
                                                 InventoryPersistencePort inventoryPersistencePort) {
        return new GetLowStockBooksService(lowStockBooksPort, inventoryPersistencePort);
    }

    @Override
    public PageResult<LowStockBook> execute(int threshold, PageQuery pageQuery) {
        return lowStockBooksPort.getLowStockBooks(threshold, pageQuery)
                .map(info -> new LowStockBook(
                        info.id(), info.title(), info.isbn(), info.stockQuantity(),
                        inventoryPersistencePort.findLastAdjustmentTime(info.id()).orElse(null)
                ));
    }
}
