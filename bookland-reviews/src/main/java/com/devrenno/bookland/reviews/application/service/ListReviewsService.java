package com.devrenno.bookland.reviews.application.service;

import com.devrenno.bookland.reviews.application.common.PageQuery;
import com.devrenno.bookland.reviews.application.common.PageResult;
import com.devrenno.bookland.reviews.application.dto.ReviewList;
import com.devrenno.bookland.reviews.application.port.in.ListReviewsUseCase;
import com.devrenno.bookland.reviews.application.port.out.ReviewPersistencePort;
import com.devrenno.bookland.reviews.domain.entity.Review;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ListReviewsService implements ListReviewsUseCase {

    private final ReviewPersistencePort reviewPersistencePort;

    private ListReviewsService(ReviewPersistencePort reviewPersistencePort) {
        this.reviewPersistencePort = reviewPersistencePort;
    }

    public static ListReviewsService create(ReviewPersistencePort reviewPersistencePort) {
        return new ListReviewsService(reviewPersistencePort);
    }

    @Override
    public ReviewList execute(UUID bookId, PageQuery pageQuery) {
        PageResult<Review> page = reviewPersistencePort.findByBookId(bookId, pageQuery);

        List<Review> all = reviewPersistencePort.findAllActiveByBookId(bookId);
        double avg = all.stream().mapToInt(Review::getRating).average().orElse(0.0);
        Map<Integer, Long> distribution = all.stream()
                .collect(Collectors.groupingBy(Review::getRating, Collectors.counting()));

        return new ReviewList(page, avg, distribution);
    }
}
