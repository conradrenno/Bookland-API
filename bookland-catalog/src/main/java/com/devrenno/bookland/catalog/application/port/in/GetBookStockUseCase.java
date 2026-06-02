package com.devrenno.bookland.catalog.application.port.in;

import java.util.UUID;

public interface GetBookStockUseCase {
    int getStock(UUID bookId);
}
