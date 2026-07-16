package com.devrenno.bookland.catalog.application.port.in;

import com.devrenno.bookland.catalog.domain.entity.Book;

import java.util.UUID;

public interface GetBookByIdUseCase {
    Book execute(UUID bookId);
}
