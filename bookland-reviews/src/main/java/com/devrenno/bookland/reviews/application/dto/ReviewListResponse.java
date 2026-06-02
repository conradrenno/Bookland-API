package com.devrenno.bookland.reviews.application.dto;

import org.springframework.data.domain.Page;

import java.util.Map;

public record ReviewListResponse(
        Page<ReviewResponse> reviews,
        double averageRating,
        Map<Integer, Long> ratingDistribution
) {}
