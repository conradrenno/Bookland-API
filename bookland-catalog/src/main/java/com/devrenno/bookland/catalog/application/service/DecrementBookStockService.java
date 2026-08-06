package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.port.in.DecrementBookStockUseCase;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;

import java.util.UUID;

public class DecrementBookStockService implements DecrementBookStockUseCase {

    private final BookPersistencePort bookPersistencePort;

    private DecrementBookStockService(BookPersistencePort bookPersistencePort) {
        this.bookPersistencePort = bookPersistencePort;
    }

    public static DecrementBookStockService create(BookPersistencePort bookPersistencePort) {
        return new DecrementBookStockService(bookPersistencePort);
    }

    /**
     * There is deliberately no read of the book first: any check performed here would be evaluated
     * against a snapshot the store may have already moved past, and a missing or inactive book is
     * indistinguishable from a sold-out one as far as the caller's next step goes — both mean
     * "cannot fulfil this line".
     */
    @Override
    public boolean tryDecrement(UUID bookId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive: " + quantity);
        }
        return bookPersistencePort.tryDecrementSellableStock(bookId, quantity);
    }
}
