package com.devrenno.bookland.reviews.application.port.in;

import com.devrenno.bookland.reviews.application.dto.ReviewListResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ListReviewsUseCase {
    ReviewListResponse execute(UUID bookId, Pageable pageable);
}
