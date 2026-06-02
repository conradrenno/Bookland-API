package com.devrenno.bookland.catalog.application.service;

import com.devrenno.bookland.catalog.application.annotation.UseCase;
import com.devrenno.bookland.catalog.application.port.in.UpdateBookAverageRatingUseCase;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.domain.entity.Book;
import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class UpdateBookAverageRatingService implements UpdateBookAverageRatingUseCase {

    private final BookPersistencePort bookPersistencePort;

    @Override
    public void updateAverageRating(UUID bookId, double newAvgRating) {
        Book book = bookPersistencePort.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));
        book.updateAverageRating(newAvgRating);
        bookPersistencePort.save(book);
    }
}
