package com.devrenno.bookland.reviews.application.dto;

import com.devrenno.bookland.reviews.application.common.PageResult;

import java.util.Map;

/**
 * Query read-model: a page of reviews for a book plus aggregate stats (average rating and the
 * rating distribution across all active reviews).
 */
public record ReviewList(
        PageResult<ReviewView> reviews,
        double averageRating,
        Map<Integer, Long> ratingDistribution
) {}
