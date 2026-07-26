package com.devrenno.bookland.reviews.application.service;

import com.devrenno.bookland.reviews.application.common.PageQuery;
import com.devrenno.bookland.reviews.application.common.PageResult;
import com.devrenno.bookland.reviews.application.dto.ReviewList;
import com.devrenno.bookland.reviews.application.dto.ReviewView;
import com.devrenno.bookland.reviews.application.port.in.ListReviewsUseCase;
import com.devrenno.bookland.reviews.application.port.out.CustomerNamePort;
import com.devrenno.bookland.reviews.application.port.out.ReviewPersistencePort;
import com.devrenno.bookland.reviews.domain.entity.Review;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ListReviewsService implements ListReviewsUseCase {

    private final ReviewPersistencePort reviewPersistencePort;
    private final CustomerNamePort customerNamePort;

    private ListReviewsService(ReviewPersistencePort reviewPersistencePort,
                               CustomerNamePort customerNamePort) {
        this.reviewPersistencePort = reviewPersistencePort;
        this.customerNamePort = customerNamePort;
    }

    public static ListReviewsService create(ReviewPersistencePort reviewPersistencePort,
                                            CustomerNamePort customerNamePort) {
        return new ListReviewsService(reviewPersistencePort, customerNamePort);
    }

    @Override
    public ReviewList execute(UUID bookId, PageQuery pageQuery) {
        ReviewViewAssembler assembler = new ReviewViewAssembler(customerNamePort);
        PageResult<ReviewView> page = reviewPersistencePort.findByBookId(bookId, pageQuery)
                .map(assembler::toView);

        List<Review> all = reviewPersistencePort.findAllActiveByBookId(bookId);
        double avg = all.stream().mapToInt(Review::getRating).average().orElse(0.0);
        Map<Integer, Long> distribution = all.stream()
                .collect(Collectors.groupingBy(Review::getRating, Collectors.counting()));

        return new ReviewList(page, avg, distribution);
    }
}
