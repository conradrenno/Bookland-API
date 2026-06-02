package com.devrenno.bookland.orders.application.port.out;

import java.util.UUID;

public interface BookStockPort {
    void adjustStock(UUID bookId, int delta);
}
