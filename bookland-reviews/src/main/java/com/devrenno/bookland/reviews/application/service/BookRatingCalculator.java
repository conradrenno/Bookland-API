package com.devrenno.bookland.reviews.application.service;

import com.devrenno.bookland.reviews.application.dto.BookRatingChanged;
import com.devrenno.bookland.reviews.domain.entity.Review;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Builds the rating event from a book's active reviews — shared by creation and moderation. */
final class BookRatingCalculator {

    private BookRatingCalculator() {
    }

    /** No active reviews left yields an average of 0, which is what the catalog shows for an unrated book. */
    static BookRatingChanged ratingChangedFor(UUID bookId, List<Review> activeReviews) {
        double average = activeReviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        return new BookRatingChanged(UUID.randomUUID(), bookId, average, activeReviews.size(), Instant.now());
    }
}
