package com.devrenno.bookland.inventory.infrastructure.adapter;

import com.devrenno.bookland.catalog.application.port.in.GetLowStockBooksUseCase;
import com.devrenno.bookland.inventory.application.common.PageQuery;
import com.devrenno.bookland.inventory.application.common.PageResult;
import com.devrenno.bookland.inventory.application.dto.LowStockBookInfo;
import com.devrenno.bookland.inventory.application.port.out.LowStockBooksPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LowStockBooksAdapter implements LowStockBooksPort {

    private final GetLowStockBooksUseCase getLowStockBooksUseCase;

    @Override
    public PageResult<LowStockBookInfo> getLowStockBooks(int threshold, PageQuery pageQuery) {
        PageRequest pageable = PageRequest.of(
                pageQuery.page(), pageQuery.size(), Sort.by("stockQuantity").ascending()
        );
        Page<LowStockBookInfo> page = getLowStockBooksUseCase.execute(threshold, pageable)
                .map(r -> new LowStockBookInfo(r.id(), r.title(), r.isbn(), r.stockQuantity()));
        return new PageResult<>(
                page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages()
        );
    }
}
