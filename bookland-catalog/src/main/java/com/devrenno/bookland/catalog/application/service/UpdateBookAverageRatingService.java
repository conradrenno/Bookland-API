package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.port.in.UpdateBookAverageRatingUseCase;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.domain.entity.Book;
import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;

import java.util.UUID;

public class UpdateBookAverageRatingService implements UpdateBookAverageRatingUseCase {

    private final BookPersistencePort bookPersistencePort;

    private UpdateBookAverageRatingService(BookPersistencePort bookPersistencePort) {
        this.bookPersistencePort = bookPersistencePort;
    }

    public static UpdateBookAverageRatingService create(BookPersistencePort bookPersistencePort) {
        return new UpdateBookAverageRatingService(bookPersistencePort);
    }

    @Override
    public void updateAverageRating(UUID bookId, double newAvgRating) {
        Book book = bookPersistencePort.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));
        book.updateAverageRating(newAvgRating);
        bookPersistencePort.save(book);
    }
}
