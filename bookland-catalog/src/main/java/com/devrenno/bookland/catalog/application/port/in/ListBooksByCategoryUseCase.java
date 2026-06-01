package com.devrenno.bookland.catalog.application.port.in;

import com.devrenno.bookland.catalog.application.dto.BookResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ListBooksByCategoryUseCase {
    Page<BookResponse> execute(UUID categoryId, Pageable pageable);
}
