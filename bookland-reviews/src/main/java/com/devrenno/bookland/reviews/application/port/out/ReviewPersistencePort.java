package com.devrenno.bookland.reviews.application.port.out;

import com.devrenno.bookland.reviews.domain.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewPersistencePort {
    Review save(Review review);
    Optional<Review> findById(UUID reviewId);
    Optional<Review> findByBookIdAndCustomerId(UUID bookId, UUID customerId);
    Page<Review> findByBookId(UUID bookId, Pageable pageable);
    List<Review> findAllActiveByBookId(UUID bookId);
}
