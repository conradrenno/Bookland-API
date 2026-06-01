package com.devrenno.bookland.catalog.application.port.in;

import java.util.UUID;

public interface RemoveBookUseCase {
    void execute(UUID bookId);
}
