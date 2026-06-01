package com.devrenno.bookland.catalog.application.port.out;

import java.util.UUID;

public interface ActiveOrderCheckPort {
    boolean hasActiveOrdersForBook(UUID bookId);
}
