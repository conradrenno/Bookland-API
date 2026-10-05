package com.devrenno.bookland.reviews.application.service;

import com.devrenno.bookland.reviews.application.port.in.ModerateReviewUseCase;
import com.devrenno.bookland.reviews.application.port.out.BookRatingEventPort;
import com.devrenno.bookland.reviews.application.port.out.ReviewPersistencePort;
import com.devrenno.bookland.reviews.application.port.out.TransactionPort;
import com.devrenno.bookland.reviews.domain.entity.Review;
import com.devrenno.bookland.reviews.domain.exception.ReviewNotFoundException;

import java.util.UUID;

public class ModerateReviewService implements ModerateReviewUseCase {

    private final ReviewPersistencePort reviewPersistencePort;
    private final BookRatingEventPort bookRatingEventPort;
    private final TransactionPort transactionPort;

    private ModerateReviewService(ReviewPersistencePort reviewPersistencePort,
                                  BookRatingEventPort bookRatingEventPort,
                                  TransactionPort transactionPort) {
        this.reviewPersistencePort = reviewPersistencePort;
        this.bookRatingEventPort = bookRatingEventPort;
        this.transactionPort = transactionPort;
    }

    public static ModerateReviewService create(ReviewPersistencePort reviewPersistencePort,
                                               BookRatingEventPort bookRatingEventPort,
                                               TransactionPort transactionPort) {
        return new ModerateReviewService(reviewPersistencePort, bookRatingEventPort, transactionPort);
    }

    @Override
    public void execute(UUID reviewId) {
        Review review = reviewPersistencePort.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));
        UUID bookId = review.getBookId();
        review.softDelete();

        // Same unit as the creation: the soft delete and its rating event commit together.
        transactionPort.inTransaction(() -> {
            reviewPersistencePort.save(review);
            bookRatingEventPort.publish(BookRatingCalculator.ratingChangedFor(
                    bookId, reviewPersistencePort.findAllActiveByBookId(bookId)));
        });
    }
}
