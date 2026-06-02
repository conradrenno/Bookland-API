package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.annotation.UseCase;
import com.devrenno.bookland.catalog.application.dto.BookResponse;
import com.devrenno.bookland.catalog.application.mapper.CatalogApplicationMapper;
import com.devrenno.bookland.catalog.application.port.in.GetLowStockBooksUseCase;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@UseCase
@RequiredArgsConstructor
public class GetLowStockBooksService implements GetLowStockBooksUseCase {

    private final BookPersistencePort bookPersistencePort;
    private final CatalogApplicationMapper mapper;

    @Override
    public Page<BookResponse> execute(int threshold, Pageable pageable) {
        return bookPersistencePort.findLowStock(threshold, pageable)
                .map(mapper::toResponse);
    }
}
