package com.devrenno.bookland.catalog.application.port.in;

import com.devrenno.bookland.catalog.application.dto.BookResponse;
import com.devrenno.bookland.catalog.application.dto.UpdateBookCommand;

import java.util.UUID;

public interface UpdateBookUseCase {
    BookResponse execute(UUID bookId, UpdateBookCommand command);
}
