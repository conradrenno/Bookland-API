package com.devrenno.bookland.catalog.application.port.in;

import com.devrenno.bookland.catalog.application.dto.UpdateBookCommand;
import com.devrenno.bookland.catalog.domain.entity.Book;

import java.util.UUID;

public interface UpdateBookUseCase {
    Book execute(UUID bookId, UpdateBookCommand command);
}
