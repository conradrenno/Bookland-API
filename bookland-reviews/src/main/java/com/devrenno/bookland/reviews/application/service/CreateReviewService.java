package com.devrenno.bookland.reviews.application.service;

import com.devrenno.bookland.reviews.application.dto.CreateReviewCommand;
import com.devrenno.bookland.reviews.application.dto.ReviewView;
import com.devrenno.bookland.reviews.application.port.in.CreateReviewUseCase;
import com.devrenno.bookland.reviews.application.port.out.BookExistsPort;
import com.devrenno.bookland.reviews.application.port.out.BookRatingEventPort;
import com.devrenno.bookland.reviews.application.port.out.PurchaseVerificationPort;
import com.devrenno.bookland.reviews.application.port.out.ReviewPersistencePort;
import com.devrenno.bookland.reviews.application.port.out.TransactionPort;
import com.devrenno.bookland.reviews.domain.entity.Review;
import com.devrenno.bookland.reviews.domain.exception.BookNotFoundException;
import com.devrenno.bookland.reviews.domain.exception.DuplicateReviewException;
import com.devrenno.bookland.reviews.domain.exception.PurchaseRequiredException;

public class CreateReviewService implements CreateReviewUseCase {

    private final ReviewPersistencePort reviewPersistencePort;
    private final BookExistsPort bookExistsPort;
    private final PurchaseVerificationPort purchaseVerificationPort;
    private final BookRatingEventPort bookRatingEventPort;
    private final TransactionPort transactionPort;

    private CreateReviewService(ReviewPersistencePort reviewPersistencePort,
                                BookExistsPort bookExistsPort,
                                PurchaseVerificationPort purchaseVerificationPort,
                                BookRatingEventPort bookRatingEventPort,
                                TransactionPort transactionPort) {
        this.reviewPersistencePort = reviewPersistencePort;
        this.bookExistsPort = bookExistsPort;
        this.purchaseVerificationPort = purchaseVerificationPort;
        this.bookRatingEventPort = bookRatingEventPort;
        this.transactionPort = transactionPort;
    }

    public static CreateReviewService create(ReviewPersistencePort reviewPersistencePort,
                                             BookExistsPort bookExistsPort,
                                             PurchaseVerificationPort purchaseVerificationPort,
                                             BookRatingEventPort bookRatingEventPort,
                                             TransactionPort transactionPort) {
        return new CreateReviewService(reviewPersistencePort, bookExistsPort,
                purchaseVerificationPort, bookRatingEventPort, transactionPort);
    }

    @Override
    public ReviewView execute(CreateReviewCommand command) {
        if (!bookExistsPort.exists(command.bookId())) {
            throw new BookNotFoundException(command.bookId());
        }
        if (!purchaseVerificationPort.hasPurchasedBook(command.customerId(), command.bookId())) {
            throw new PurchaseRequiredException(command.customerId(), command.bookId());
        }
        reviewPersistencePort.findByBookIdAndCustomerId(command.bookId(), command.customerId())
                .ifPresent(r -> { throw new DuplicateReviewException(command.customerId(), command.bookId()); });

        // The name arrives with the caller's token and is stored: the review carries its author's
        // name from now on, and neither creating nor listing it asks the user module anything.
        Review review = Review.create(command.bookId(), command.customerId(), command.customerName(),
                command.rating(), command.comment());

        // The review and its rating event are stored together or not at all: the event goes to the
        // outbox in this same transaction, and reaches Kafka afterwards. The lookups above stay
        // outside it, so no transaction is held open across calls to other modules.
        Review saved = transactionPort.inTransaction(() -> {
            Review stored = reviewPersistencePort.save(review);
            bookRatingEventPort.publish(BookRatingCalculator.ratingChangedFor(
                    command.bookId(), reviewPersistencePort.findAllActiveByBookId(command.bookId())));
            return stored;
        });

        return ReviewView.from(saved);
    }
}
