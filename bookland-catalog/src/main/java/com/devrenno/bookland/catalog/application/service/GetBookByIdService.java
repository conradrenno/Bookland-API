package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.annotation.UseCase;
import com.devrenno.bookland.catalog.application.dto.BookResponse;
import com.devrenno.bookland.catalog.application.mapper.CatalogApplicationMapper;
import com.devrenno.bookland.catalog.application.port.in.GetBookByIdUseCase;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class GetBookByIdService implements GetBookByIdUseCase {

    private final BookPersistencePort bookPersistencePort;
    private final CatalogApplicationMapper mapper;

    @Override
    public BookResponse execute(UUID bookId) {
        return bookPersistencePort.findById(bookId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new BookNotFoundException(bookId));
    }
}
