package com.devrenno.bookland.reviews.adapters.controller;

import com.devrenno.bookland.reviews.adapters.presenter.ReviewPresenter;
import com.devrenno.bookland.reviews.adapters.viewmodel.ReviewListViewModel;
import com.devrenno.bookland.reviews.adapters.viewmodel.ReviewViewModel;
import com.devrenno.bookland.reviews.application.common.PageQuery;
import com.devrenno.bookland.reviews.application.dto.CreateReviewCommand;
import com.devrenno.bookland.reviews.application.port.in.CreateReviewUseCase;
import com.devrenno.bookland.reviews.application.port.in.ListReviewsUseCase;
import com.devrenno.bookland.reviews.application.port.in.ModerateReviewUseCase;
import com.devrenno.bookland.reviews.application.port.out.BookExistsPort;
import com.devrenno.bookland.reviews.application.port.out.BookRatingUpdatePort;
import com.devrenno.bookland.reviews.application.port.out.PurchaseVerificationPort;
import com.devrenno.bookland.reviews.application.port.out.ReviewPersistencePort;
import com.devrenno.bookland.reviews.application.service.CreateReviewService;
import com.devrenno.bookland.reviews.application.service.ListReviewsService;
import com.devrenno.bookland.reviews.application.service.ModerateReviewService;

import java.util.UUID;

/**
 * Internal controller: orchestrates the reviews use cases and delegates to the Presenter.
 * Also the module's composition root — its create(...) factory wires the use cases from the
 * outbound ports it receives (as interfaces). Framework-free.
 */
public class ReviewController {

    private final CreateReviewUseCase createReviewUseCase;
    private final ListReviewsUseCase listReviewsUseCase;
    private final ModerateReviewUseCase moderateReviewUseCase;
    private final ReviewPresenter presenter;

    private ReviewController(CreateReviewUseCase createReviewUseCase,
                            ListReviewsUseCase listReviewsUseCase,
                            ModerateReviewUseCase moderateReviewUseCase,
                            ReviewPresenter presenter) {
        this.createReviewUseCase = createReviewUseCase;
        this.listReviewsUseCase = listReviewsUseCase;
        this.moderateReviewUseCase = moderateReviewUseCase;
        this.presenter = presenter;
    }

    public static ReviewController create(ReviewPersistencePort reviewPersistencePort,
                                          BookExistsPort bookExistsPort,
                                          PurchaseVerificationPort purchaseVerificationPort,
                                          BookRatingUpdatePort bookRatingUpdatePort) {
        return new ReviewController(
                CreateReviewService.create(reviewPersistencePort, bookExistsPort,
                        purchaseVerificationPort, bookRatingUpdatePort),
                ListReviewsService.create(reviewPersistencePort),
                ModerateReviewService.create(reviewPersistencePort, bookRatingUpdatePort),
                ReviewPresenter.create()
        );
    }

    public ReviewViewModel create(CreateReviewCommand command) {
        return presenter.present(createReviewUseCase.execute(command));
    }

    public ReviewListViewModel list(UUID bookId, PageQuery pageQuery) {
        return presenter.present(listReviewsUseCase.execute(bookId, pageQuery));
    }

    public void moderate(UUID reviewId) {
        moderateReviewUseCase.execute(reviewId);
    }
}
