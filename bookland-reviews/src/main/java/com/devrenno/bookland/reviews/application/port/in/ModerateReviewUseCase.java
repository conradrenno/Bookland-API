package com.devrenno.bookland.reviews.application.port.in;

import java.util.UUID;

public interface ModerateReviewUseCase {
    void execute(UUID reviewId);
}
