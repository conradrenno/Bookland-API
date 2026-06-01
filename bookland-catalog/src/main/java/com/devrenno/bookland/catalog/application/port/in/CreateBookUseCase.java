package com.devrenno.bookland.catalog.application.port.in;

import com.devrenno.bookland.catalog.application.dto.BookResponse;
import com.devrenno.bookland.catalog.application.dto.CreateBookCommand;

public interface CreateBookUseCase {
    BookResponse execute(CreateBookCommand command);
}
