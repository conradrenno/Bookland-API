package com.devrenno.bookland.catalog.application.port.in;

import java.util.UUID;

public interface AdjustBookStockUseCase {
    int adjustStock(UUID bookId, int delta);
}
