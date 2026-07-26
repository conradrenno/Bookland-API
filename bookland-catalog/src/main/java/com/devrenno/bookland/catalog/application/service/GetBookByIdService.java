package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.port.in.GetBookByIdUseCase;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.domain.entity.Book;
import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;

import java.util.UUID;

public class GetBookByIdService implements GetBookByIdUseCase {

    private final BookPersistencePort bookPersistencePort;

    private GetBookByIdService(BookPersistencePort bookPersistencePort) {
        this.bookPersistencePort = bookPersistencePort;
    }

    public static GetBookByIdService create(BookPersistencePort bookPersistencePort) {
        return new GetBookByIdService(bookPersistencePort);
    }

    /**
     * Public book lookup: a deactivated (soft-deleted) book is indistinguishable from a missing one,
     * so it must not be readable, addable to a cart/wishlist or checked out. Admin flows that need
     * an inactive book (update / cover upload / removal) go through BookPersistencePort directly.
     */
    @Override
    public Book execute(UUID bookId) {
        return bookPersistencePort.findById(bookId)
                .filter(Book::isActive)
                .orElseThrow(() -> new BookNotFoundException(bookId));
    }
}
