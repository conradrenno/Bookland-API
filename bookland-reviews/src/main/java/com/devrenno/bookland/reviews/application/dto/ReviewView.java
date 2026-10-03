package com.devrenno.bookland.reviews.application.dto;

import com.devrenno.bookland.reviews.domain.entity.Review;

import java.time.Instant;
import java.util.UUID;

/**
 * Query read-model: a review labelled with its author's display name (null when the author could
 * not be resolved — the delivery layer decides how to render that). The name is the one stored on
 * the review, so building this view needs no lookup in another module.
 */
public record ReviewView(
        UUID id,
        UUID bookId,
        UUID customerId,
        String customerName,
        int rating,
        String comment,
        Instant createdAt
) {

    public static ReviewView from(Review review) {
        return new ReviewView(
                review.getId(), review.getBookId(), review.getCustomerId(), review.getCustomerName(),
                review.getRating(), review.getComment(), review.getCreatedAt()
        );
    }
}
