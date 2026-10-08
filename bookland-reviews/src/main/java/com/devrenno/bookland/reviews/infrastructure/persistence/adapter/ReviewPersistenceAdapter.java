package com.devrenno.bookland.reviews.infrastructure.persistence.adapter;

import com.devrenno.bookland.reviews.application.common.PageQuery;
import com.devrenno.bookland.reviews.application.common.PageResult;
import com.devrenno.bookland.reviews.application.port.out.ReviewPersistencePort;
import com.devrenno.bookland.reviews.domain.entity.Review;
import com.devrenno.bookland.reviews.domain.exception.DuplicateReviewException;
import com.devrenno.bookland.reviews.infrastructure.persistence.entity.ReviewJpaEntity;
import com.devrenno.bookland.reviews.infrastructure.persistence.repository.ReviewJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ReviewPersistenceAdapter implements ReviewPersistencePort {

    /** Newest first; the id breaks ties so a page boundary never repeats or drops a review. */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private static final String ONE_LIVE_REVIEW_INDEX = "uk_reviews_live_review";

    private final ReviewJpaRepository reviewRepository;

    /**
     * Flushed at once so the database's answer arrives here, inside the caller's transaction: two
     * reviews of one book by one customer submitted together both pass the service's check, and the
     * unique index refuses the second — reported as the same {@link DuplicateReviewException} the
     * check would have thrown, a 409 instead of a 500.
     */
    @Override
    public Review save(Review review) {
        try {
            return toDomain(reviewRepository.saveAndFlush(toEntity(review)));
        } catch (DataIntegrityViolationException e) {
            if (isOneLiveReviewViolation(e)) {
                throw new DuplicateReviewException(review.getCustomerId(), review.getBookId());
            }
            throw e;
        }
    }

    private static boolean isOneLiveReviewViolation(DataIntegrityViolationException e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null && cause.getMessage().toLowerCase().contains(ONE_LIVE_REVIEW_INDEX)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Optional<Review> findById(UUID reviewId) {
        return reviewRepository.findById(reviewId).map(this::toDomain);
    }

    @Override
    public Optional<Review> findByBookIdAndCustomerId(UUID bookId, UUID customerId) {
        return reviewRepository.findByBookIdAndCustomerIdAndDeletedFalse(bookId, customerId).map(this::toDomain);
    }

    @Override
    public PageResult<Review> findByBookId(UUID bookId, PageQuery pageQuery) {
        Page<Review> page = reviewRepository
                .findByBookIdAndDeletedFalse(bookId, PageRequest.of(pageQuery.page(), pageQuery.size(), NEWEST_FIRST))
                .map(this::toDomain);
        return new PageResult<>(
                page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages()
        );
    }

    @Override
    public List<Review> findAllActiveByBookId(UUID bookId) {
        return reviewRepository.findAllByBookIdAndDeletedFalse(bookId).stream().map(this::toDomain).toList();
    }

    private ReviewJpaEntity toEntity(Review review) {
        return ReviewJpaEntity.builder()
                .id(review.getId())
                .bookId(review.getBookId())
                .customerId(review.getCustomerId())
                .customerName(review.getCustomerName())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .deleted(review.isDeleted())
                .liveCustomerId(review.isDeleted() ? null : review.getCustomerId())
                .build();
    }

    private Review toDomain(ReviewJpaEntity entity) {
        return Review.reconstitute(
                entity.getId(),
                entity.getBookId(),
                entity.getCustomerId(),
                entity.getCustomerName(),
                entity.getRating(),
                entity.getComment(),
                entity.getCreatedAt(),
                entity.isDeleted()
        );
    }
}
