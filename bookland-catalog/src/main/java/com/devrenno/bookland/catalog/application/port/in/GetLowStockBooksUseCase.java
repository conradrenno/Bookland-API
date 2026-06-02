package com.devrenno.bookland.catalog.application.port.in;

import com.devrenno.bookland.catalog.application.dto.BookResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GetLowStockBooksUseCase {
    Page<BookResponse> execute(int threshold, Pageable pageable);
}
