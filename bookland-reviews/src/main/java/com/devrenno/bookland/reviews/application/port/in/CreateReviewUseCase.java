package com.devrenno.bookland.reviews.application.port.in;

import com.devrenno.bookland.reviews.application.dto.CreateReviewCommand;
import com.devrenno.bookland.reviews.domain.entity.Review;

public interface CreateReviewUseCase {
    Review execute(CreateReviewCommand command);
}
