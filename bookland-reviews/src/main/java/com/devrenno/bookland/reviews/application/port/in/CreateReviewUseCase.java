package com.devrenno.bookland.reviews.application.port.in;

import com.devrenno.bookland.reviews.application.dto.CreateReviewCommand;
import com.devrenno.bookland.reviews.application.dto.ReviewView;

public interface CreateReviewUseCase {
    ReviewView execute(CreateReviewCommand command);
}
