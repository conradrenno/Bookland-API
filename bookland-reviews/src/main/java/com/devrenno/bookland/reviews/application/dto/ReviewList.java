package com.devrenno.bookland.reviews.application.dto;

import com.devrenno.bookland.reviews.application.common.PageResult;
import com.devrenno.bookland.reviews.domain.entity.Review;

import java.util.Map;

/**
 * Query read-model: a page of reviews for a book plus aggregate stats (average rating and the
 * rating distribution across all active reviews).
 */
public record ReviewList(
        PageResult<Review> reviews,
        double averageRating,
        Map<Integer, Long> ratingDistribution
) {}
