package com.devrenno.bookland.catalog.application.port.in;

import com.devrenno.bookland.catalog.application.dto.CreateBookCommand;
import com.devrenno.bookland.catalog.domain.entity.Book;

public interface CreateBookUseCase {
    Book execute(CreateBookCommand command);
}
