package com.devrenno.bookland.reviews.application.service;

import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;
import com.devrenno.bookland.reviews.application.dto.CreateReviewCommand;
import com.devrenno.bookland.reviews.application.port.in.CreateReviewUseCase;
import com.devrenno.bookland.reviews.application.port.out.BookExistsPort;
import com.devrenno.bookland.reviews.application.port.out.BookRatingUpdatePort;
import com.devrenno.bookland.reviews.application.port.out.PurchaseVerificationPort;
import com.devrenno.bookland.reviews.application.port.out.ReviewPersistencePort;
import com.devrenno.bookland.reviews.domain.entity.Review;
import com.devrenno.bookland.reviews.domain.exception.DuplicateReviewException;
import com.devrenno.bookland.reviews.domain.exception.PurchaseRequiredException;

import java.util.List;
import java.util.UUID;

public class CreateReviewService implements CreateReviewUseCase {

    private final ReviewPersistencePort reviewPersistencePort;
    private final BookExistsPort bookExistsPort;
    private final PurchaseVerificationPort purchaseVerificationPort;
    private final BookRatingUpdatePort bookRatingUpdatePort;

    private CreateReviewService(ReviewPersistencePort reviewPersistencePort,
                                BookExistsPort bookExistsPort,
                                PurchaseVerificationPort purchaseVerificationPort,
                                BookRatingUpdatePort bookRatingUpdatePort) {
        this.reviewPersistencePort = reviewPersistencePort;
        this.bookExistsPort = bookExistsPort;
        this.purchaseVerificationPort = purchaseVerificationPort;
        this.bookRatingUpdatePort = bookRatingUpdatePort;
    }

    public static CreateReviewService create(ReviewPersistencePort reviewPersistencePort,
                                             BookExistsPort bookExistsPort,
                                             PurchaseVerificationPort purchaseVerificationPort,
                                             BookRatingUpdatePort bookRatingUpdatePort) {
        return new CreateReviewService(reviewPersistencePort, bookExistsPort,
                purchaseVerificationPort, bookRatingUpdatePort);
    }

    @Override
    public Review execute(CreateReviewCommand command) {
        if (!bookExistsPort.exists(command.bookId())) {
            throw new BookNotFoundException(command.bookId());
        }
        if (!purchaseVerificationPort.hasPurchasedBook(command.customerId(), command.bookId())) {
            throw new PurchaseRequiredException(command.customerId(), command.bookId());
        }
        reviewPersistencePort.findByBookIdAndCustomerId(command.bookId(), command.customerId())
                .ifPresent(r -> { throw new DuplicateReviewException(command.customerId(), command.bookId()); });

        Review review = Review.create(command.bookId(), command.customerId(), command.rating(), command.comment());
        Review saved = reviewPersistencePort.save(review);

        recalculateRating(command.bookId());

        return saved;
    }

    private void recalculateRating(UUID bookId) {
        List<Review> active = reviewPersistencePort.findAllActiveByBookId(bookId);
        if (active.isEmpty()) return;
        double avg = active.stream().mapToInt(Review::getRating).average().orElse(0.0);
        bookRatingUpdatePort.updateRating(bookId, avg);
    }
}
