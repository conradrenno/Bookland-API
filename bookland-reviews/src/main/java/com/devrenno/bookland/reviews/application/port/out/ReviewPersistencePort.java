package com.devrenno.bookland.reviews.application.port.out;

import com.devrenno.bookland.reviews.application.common.PageQuery;
import com.devrenno.bookland.reviews.application.common.PageResult;
import com.devrenno.bookland.reviews.domain.entity.Review;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewPersistencePort {
    Review save(Review review);
    Optional<Review> findById(UUID reviewId);
    Optional<Review> findByBookIdAndCustomerId(UUID bookId, UUID customerId);
    PageResult<Review> findByBookId(UUID bookId, PageQuery pageQuery);
    List<Review> findAllActiveByBookId(UUID bookId);
}
