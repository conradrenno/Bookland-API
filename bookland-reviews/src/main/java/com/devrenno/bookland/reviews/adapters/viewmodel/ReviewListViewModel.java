package com.devrenno.bookland.reviews.adapters.viewmodel;

import com.devrenno.bookland.reviews.application.common.PageResult;

import java.util.Map;

public record ReviewListViewModel(
        PageResult<ReviewViewModel> reviews,
        double averageRating,
        Map<Integer, Long> ratingDistribution
) {}
