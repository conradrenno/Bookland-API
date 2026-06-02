package com.devrenno.bookland.reviews.application.service;

import com.devrenno.bookland.reviews.application.annotation.UseCase;
import com.devrenno.bookland.reviews.application.port.in.ModerateReviewUseCase;
import com.devrenno.bookland.reviews.application.port.out.BookRatingUpdatePort;
import com.devrenno.bookland.reviews.application.port.out.ReviewPersistencePort;
import com.devrenno.bookland.reviews.domain.entity.Review;
import com.devrenno.bookland.reviews.domain.exception.ReviewNotFoundException;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class ModerateReviewService implements ModerateReviewUseCase {

    private final ReviewPersistencePort reviewPersistencePort;
    private final BookRatingUpdatePort bookRatingUpdatePort;

    @Override
    public void execute(UUID reviewId) {
        Review review = reviewPersistencePort.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));
        UUID bookId = review.getBookId();
        review.softDelete();
        reviewPersistencePort.save(review);

        List<Review> active = reviewPersistencePort.findAllActiveByBookId(bookId);
        double avg = active.stream().mapToInt(Review::getRating).average().orElse(0.0);
        bookRatingUpdatePort.updateRating(bookId, avg);
    }
}
