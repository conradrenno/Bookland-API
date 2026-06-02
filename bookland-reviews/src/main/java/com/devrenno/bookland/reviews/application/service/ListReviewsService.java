package com.devrenno.bookland.reviews.application.service;

import com.devrenno.bookland.reviews.application.annotation.UseCase;
import com.devrenno.bookland.reviews.application.dto.ReviewListResponse;
import com.devrenno.bookland.reviews.application.dto.ReviewResponse;
import com.devrenno.bookland.reviews.application.port.in.ListReviewsUseCase;
import com.devrenno.bookland.reviews.application.port.out.ReviewPersistencePort;
import com.devrenno.bookland.reviews.domain.entity.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@UseCase
@RequiredArgsConstructor
public class ListReviewsService implements ListReviewsUseCase {

    private final ReviewPersistencePort reviewPersistencePort;

    @Override
    public ReviewListResponse execute(UUID bookId, Pageable pageable) {
        Page<ReviewResponse> page = reviewPersistencePort.findByBookId(bookId, pageable)
                .map(CreateReviewService::toResponse);

        List<Review> all = reviewPersistencePort.findAllActiveByBookId(bookId);
        double avg = all.stream().mapToInt(Review::getRating).average().orElse(0.0);
        Map<Integer, Long> distribution = all.stream()
                .collect(Collectors.groupingBy(Review::getRating, Collectors.counting()));

        return new ReviewListResponse(page, avg, distribution);
    }
}
