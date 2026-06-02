package com.devrenno.bookland.inventory.infrastructure.adapter;

import com.devrenno.bookland.catalog.application.port.in.GetLowStockBooksUseCase;
import com.devrenno.bookland.inventory.application.dto.LowStockBookInfo;
import com.devrenno.bookland.inventory.application.port.out.LowStockBooksPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LowStockBooksAdapter implements LowStockBooksPort {

    private final GetLowStockBooksUseCase getLowStockBooksUseCase;

    @Override
    public Page<LowStockBookInfo> getLowStockBooks(int threshold, Pageable pageable) {
        return getLowStockBooksUseCase.execute(threshold, pageable)
                .map(r -> new LowStockBookInfo(r.id(), r.title(), r.isbn(), r.stockQuantity()));
    }
}
