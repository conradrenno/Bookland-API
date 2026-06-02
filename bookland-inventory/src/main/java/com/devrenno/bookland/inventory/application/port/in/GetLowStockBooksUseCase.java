package com.devrenno.bookland.inventory.application.port.in;

import com.devrenno.bookland.inventory.application.dto.LowStockBookResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GetLowStockBooksUseCase {
    Page<LowStockBookResponse> execute(int threshold, Pageable pageable);
}
