package com.devrenno.bookland.reviews.adapters.presenter;

import com.devrenno.bookland.reviews.adapters.viewmodel.ReviewListViewModel;
import com.devrenno.bookland.reviews.adapters.viewmodel.ReviewViewModel;
import com.devrenno.bookland.reviews.application.dto.ReviewList;
import com.devrenno.bookland.reviews.application.dto.ReviewView;

/**
 * Transforms domain reviews / query read-models into delivery-facing view models. Plain Java.
 */
public class ReviewPresenter {

    private ReviewPresenter() {
    }

    public static ReviewPresenter create() {
        return new ReviewPresenter();
    }

    public ReviewViewModel present(ReviewView review) {
        return new ReviewViewModel(
                review.id(),
                review.bookId(),
                review.customerId(),
                review.customerName(),
                review.rating(),
                review.comment(),
                review.createdAt()
        );
    }

    public ReviewListViewModel present(ReviewList reviewList) {
        return new ReviewListViewModel(
                reviewList.reviews().map(this::present),
                reviewList.averageRating(),
                reviewList.ratingDistribution()
        );
    }
}
