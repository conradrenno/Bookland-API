package com.devrenno.bookland.catalog.application.port.in;

import com.devrenno.bookland.catalog.application.dto.BookResponse;

import java.util.UUID;

public interface GetBookByIdUseCase {
    BookResponse execute(UUID bookId);
}
