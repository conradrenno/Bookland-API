package com.devrenno.bookland.reviews.infrastructure.adapter;

import com.devrenno.bookland.catalog.application.port.in.UpdateBookAverageRatingUseCase;
import com.devrenno.bookland.reviews.application.port.out.BookRatingUpdatePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BookRatingUpdateAdapter implements BookRatingUpdatePort {

    private final UpdateBookAverageRatingUseCase updateBookAverageRatingUseCase;

    @Override
    public void updateRating(UUID bookId, double newAvgRating) {
        updateBookAverageRatingUseCase.updateAverageRating(bookId, newAvgRating);
    }
}
