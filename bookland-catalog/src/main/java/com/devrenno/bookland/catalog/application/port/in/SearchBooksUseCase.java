package com.devrenno.bookland.catalog.application.port.in;

import com.devrenno.bookland.catalog.application.dto.BookResponse;
import com.devrenno.bookland.catalog.application.dto.BookSearchQuery;
import org.springframework.data.domain.Page;

public interface SearchBooksUseCase {
    Page<BookResponse> execute(BookSearchQuery query);
}
