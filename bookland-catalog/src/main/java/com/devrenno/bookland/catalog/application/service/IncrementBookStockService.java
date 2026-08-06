package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.port.in.IncrementBookStockUseCase;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;

import java.util.UUID;

public class IncrementBookStockService implements IncrementBookStockUseCase {

    private final BookPersistencePort bookPersistencePort;

    private IncrementBookStockService(BookPersistencePort bookPersistencePort) {
        this.bookPersistencePort = bookPersistencePort;
    }

    public static IncrementBookStockService create(BookPersistencePort bookPersistencePort) {
        return new IncrementBookStockService(bookPersistencePort);
    }

    /**
     * A book that matched no row still raises {@link BookNotFoundException}, as the read-modify-write
     * path it replaces did: soft deletion keeps the row, so a miss here means the id never existed
     * and silently discarding the units would be worse than failing the cancellation.
     */
    @Override
    public void increment(UUID bookId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive: " + quantity);
        }
        if (!bookPersistencePort.incrementStock(bookId, quantity)) {
            throw new BookNotFoundException(bookId);
        }
    }
}
