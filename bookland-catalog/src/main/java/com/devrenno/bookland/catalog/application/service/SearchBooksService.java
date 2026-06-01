package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.annotation.UseCase;
import com.devrenno.bookland.catalog.application.dto.BookResponse;
import com.devrenno.bookland.catalog.application.dto.BookSearchQuery;
import com.devrenno.bookland.catalog.application.mapper.CatalogApplicationMapper;
import com.devrenno.bookland.catalog.application.port.in.SearchBooksUseCase;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;

@UseCase
@RequiredArgsConstructor
public class SearchBooksService implements SearchBooksUseCase {

    private final BookPersistencePort bookPersistencePort;
    private final CatalogApplicationMapper mapper;

    @Override
    public Page<BookResponse> execute(BookSearchQuery query) {
        return bookPersistencePort.search(query).map(mapper::toResponse);
    }
}
