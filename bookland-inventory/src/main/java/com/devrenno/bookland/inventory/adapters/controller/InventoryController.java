package com.devrenno.bookland.inventory.adapters.controller;

import com.devrenno.bookland.inventory.adapters.presenter.InventoryPresenter;
import com.devrenno.bookland.inventory.adapters.viewmodel.InventoryEntryViewModel;
import com.devrenno.bookland.inventory.adapters.viewmodel.LowStockBookViewModel;
import com.devrenno.bookland.inventory.application.common.PageQuery;
import com.devrenno.bookland.inventory.application.common.PageResult;
import com.devrenno.bookland.inventory.application.dto.AdjustInventoryCommand;
import com.devrenno.bookland.inventory.application.port.in.AdjustInventoryUseCase;
import com.devrenno.bookland.inventory.application.port.in.GetInventoryHistoryUseCase;
import com.devrenno.bookland.inventory.application.port.in.GetLowStockBooksUseCase;
import com.devrenno.bookland.inventory.application.port.out.BookStockAdjustmentPort;
import com.devrenno.bookland.inventory.application.port.out.InventoryPersistencePort;
import com.devrenno.bookland.inventory.application.port.out.LowStockBooksPort;
import com.devrenno.bookland.inventory.application.service.AdjustInventoryService;
import com.devrenno.bookland.inventory.application.service.GetInventoryHistoryService;
import com.devrenno.bookland.inventory.application.service.GetLowStockBooksService;

import java.util.UUID;

/**
 * Internal controller: orchestrates the inventory use cases and delegates to the Presenter.
 * Also the module's composition root — its create(...) factory wires the use cases from the
 * outbound ports it receives (as interfaces). Framework-free.
 */
public class InventoryController {

    private final AdjustInventoryUseCase adjustInventoryUseCase;
    private final GetInventoryHistoryUseCase getInventoryHistoryUseCase;
    private final GetLowStockBooksUseCase getLowStockBooksUseCase;
    private final InventoryPresenter presenter;

    private InventoryController(AdjustInventoryUseCase adjustInventoryUseCase,
                              GetInventoryHistoryUseCase getInventoryHistoryUseCase,
                              GetLowStockBooksUseCase getLowStockBooksUseCase,
                              InventoryPresenter presenter) {
        this.adjustInventoryUseCase = adjustInventoryUseCase;
        this.getInventoryHistoryUseCase = getInventoryHistoryUseCase;
        this.getLowStockBooksUseCase = getLowStockBooksUseCase;
        this.presenter = presenter;
    }

    public static InventoryController create(BookStockAdjustmentPort bookStockAdjustmentPort,
                                             InventoryPersistencePort inventoryPersistencePort,
                                             LowStockBooksPort lowStockBooksPort) {
        return new InventoryController(
                AdjustInventoryService.create(bookStockAdjustmentPort, inventoryPersistencePort),
                GetInventoryHistoryService.create(inventoryPersistencePort),
                GetLowStockBooksService.create(lowStockBooksPort, inventoryPersistencePort),
                InventoryPresenter.create()
        );
    }

    public InventoryEntryViewModel adjust(AdjustInventoryCommand command) {
        return presenter.present(adjustInventoryUseCase.execute(command));
    }

    public PageResult<InventoryEntryViewModel> history(UUID bookId, PageQuery pageQuery) {
        return getInventoryHistoryUseCase.execute(bookId, pageQuery).map(presenter::present);
    }

    public PageResult<LowStockBookViewModel> lowStock(int threshold, PageQuery pageQuery) {
        return getLowStockBooksUseCase.execute(threshold, pageQuery).map(presenter::present);
    }
}
