package com.devrenno.bookland.inventory.infrastructure.adapter;

import com.devrenno.bookland.catalog.application.port.in.GetLowStockBooksUseCase;
import com.devrenno.bookland.catalog.domain.entity.Book;
import com.devrenno.bookland.inventory.application.common.PageQuery;
import com.devrenno.bookland.inventory.application.common.PageResult;
import com.devrenno.bookland.inventory.application.dto.LowStockBookInfo;
import com.devrenno.bookland.inventory.application.port.out.LowStockBooksPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LowStockBooksAdapter implements LowStockBooksPort {

    private final GetLowStockBooksUseCase getLowStockBooksUseCase;

    @Override
    public PageResult<LowStockBookInfo> getLowStockBooks(int threshold, PageQuery pageQuery) {
        var result = getLowStockBooksUseCase.execute(
                threshold,
                com.devrenno.bookland.catalog.application.common.PageQuery.of(pageQuery.page(), pageQuery.size())
        );
        return new PageResult<>(
                result.content().stream().map(LowStockBooksAdapter::toInfo).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages()
        );
    }

    private static LowStockBookInfo toInfo(Book book) {
        return new LowStockBookInfo(
                book.getId().value(), book.getTitle(), book.getIsbn().value(),
                book.getCoverImageUrl(), book.getStockQuantity()
        );
    }
}
