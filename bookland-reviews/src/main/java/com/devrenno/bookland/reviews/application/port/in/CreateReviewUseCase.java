package com.devrenno.bookland.reviews.application.port.in;

import com.devrenno.bookland.reviews.application.dto.CreateReviewCommand;
import com.devrenno.bookland.reviews.application.dto.ReviewResponse;

public interface CreateReviewUseCase {
    ReviewResponse execute(CreateReviewCommand command);
}
