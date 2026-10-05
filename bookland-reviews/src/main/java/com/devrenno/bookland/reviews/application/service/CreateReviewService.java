package com.devrenno.bookland.reviews.application.service;

import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;
import com.devrenno.bookland.reviews.application.dto.CreateReviewCommand;
import com.devrenno.bookland.reviews.application.dto.ReviewView;
import com.devrenno.bookland.reviews.application.port.in.CreateReviewUseCase;
import com.devrenno.bookland.reviews.application.port.out.BookExistsPort;
import com.devrenno.bookland.reviews.application.port.out.BookRatingEventPort;
import com.devrenno.bookland.reviews.application.port.out.CustomerNamePort;
import com.devrenno.bookland.reviews.application.port.out.PurchaseVerificationPort;
import com.devrenno.bookland.reviews.application.port.out.ReviewPersistencePort;
import com.devrenno.bookland.reviews.domain.entity.Review;
import com.devrenno.bookland.reviews.domain.exception.DuplicateReviewException;
import com.devrenno.bookland.reviews.domain.exception.PurchaseRequiredException;

public class CreateReviewService implements CreateReviewUseCase {

    private final ReviewPersistencePort reviewPersistencePort;
    private final BookExistsPort bookExistsPort;
    private final PurchaseVerificationPort purchaseVerificationPort;
    private final BookRatingEventPort bookRatingEventPort;
    private final CustomerNamePort customerNamePort;

    private CreateReviewService(ReviewPersistencePort reviewPersistencePort,
                                BookExistsPort bookExistsPort,
                                PurchaseVerificationPort purchaseVerificationPort,
                                BookRatingEventPort bookRatingEventPort,
                                CustomerNamePort customerNamePort) {
        this.reviewPersistencePort = reviewPersistencePort;
        this.bookExistsPort = bookExistsPort;
        this.purchaseVerificationPort = purchaseVerificationPort;
        this.bookRatingEventPort = bookRatingEventPort;
        this.customerNamePort = customerNamePort;
    }

    public static CreateReviewService create(ReviewPersistencePort reviewPersistencePort,
                                             BookExistsPort bookExistsPort,
                                             PurchaseVerificationPort purchaseVerificationPort,
                                             BookRatingEventPort bookRatingEventPort,
                                             CustomerNamePort customerNamePort) {
        return new CreateReviewService(reviewPersistencePort, bookExistsPort,
                purchaseVerificationPort, bookRatingEventPort, customerNamePort);
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

        // Looked up once, here, and stored: from now on the review carries its author's name and
        // listing it asks nobody. A failed lookup fails the creation rather than storing a null
        // that nothing would ever correct.
        String customerName = customerNamePort.getCustomerName(command.customerId());
        Review review = Review.create(command.bookId(), command.customerId(), customerName,
                command.rating(), command.comment());
        Review saved = reviewPersistencePort.save(review);

        bookRatingEventPort.publish(BookRatingCalculator.ratingChangedFor(
                command.bookId(), reviewPersistencePort.findAllActiveByBookId(command.bookId())));

        return ReviewView.from(saved);
    }
}
