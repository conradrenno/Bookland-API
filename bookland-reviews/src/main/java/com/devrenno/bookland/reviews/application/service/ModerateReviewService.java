package com.devrenno.bookland.reviews.application.service;

import com.devrenno.bookland.reviews.application.port.in.ModerateReviewUseCase;
import com.devrenno.bookland.reviews.application.port.out.BookRatingEventPort;
import com.devrenno.bookland.reviews.application.port.out.ReviewPersistencePort;
import com.devrenno.bookland.reviews.domain.entity.Review;
import com.devrenno.bookland.reviews.domain.exception.ReviewNotFoundException;

import java.util.UUID;

public class ModerateReviewService implements ModerateReviewUseCase {

    private final ReviewPersistencePort reviewPersistencePort;
    private final BookRatingEventPort bookRatingEventPort;

    private ModerateReviewService(ReviewPersistencePort reviewPersistencePort,
                                  BookRatingEventPort bookRatingEventPort) {
        this.reviewPersistencePort = reviewPersistencePort;
        this.bookRatingEventPort = bookRatingEventPort;
    }

    public static ModerateReviewService create(ReviewPersistencePort reviewPersistencePort,
                                               BookRatingEventPort bookRatingEventPort) {
        return new ModerateReviewService(reviewPersistencePort, bookRatingEventPort);
    }

    @Override
    public void execute(UUID reviewId) {
        Review review = reviewPersistencePort.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));
        UUID bookId = review.getBookId();
        review.softDelete();
        reviewPersistencePort.save(review);

        bookRatingEventPort.publish(BookRatingCalculator.ratingChangedFor(
                bookId, reviewPersistencePort.findAllActiveByBookId(bookId)));
    }
}
