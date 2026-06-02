package com.devrenno.bookland.inventory.infrastructure.adapter;

import com.devrenno.bookland.catalog.application.port.in.AdjustBookStockUseCase;
import com.devrenno.bookland.catalog.application.port.in.GetBookStockUseCase;
import com.devrenno.bookland.inventory.application.port.out.BookStockAdjustmentPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BookStockAdjustmentAdapter implements BookStockAdjustmentPort {

    private final GetBookStockUseCase getBookStockUseCase;
    private final AdjustBookStockUseCase adjustBookStockUseCase;

    @Override
    public int getCurrentStock(UUID bookId) {
        return getBookStockUseCase.getStock(bookId);
    }

    @Override
    public int adjustStock(UUID bookId, int delta) {
        return adjustBookStockUseCase.adjustStock(bookId, delta);
    }
}
