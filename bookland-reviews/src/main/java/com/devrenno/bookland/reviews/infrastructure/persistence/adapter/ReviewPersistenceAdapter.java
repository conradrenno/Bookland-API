package com.devrenno.bookland.reviews.infrastructure.persistence.adapter;

import com.devrenno.bookland.reviews.application.common.PageQuery;
import com.devrenno.bookland.reviews.application.common.PageResult;
import com.devrenno.bookland.reviews.application.port.out.ReviewPersistencePort;
import com.devrenno.bookland.reviews.domain.entity.Review;
import com.devrenno.bookland.reviews.infrastructure.persistence.entity.ReviewJpaEntity;
import com.devrenno.bookland.reviews.infrastructure.persistence.repository.ReviewJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ReviewPersistenceAdapter implements ReviewPersistencePort {

    private final ReviewJpaRepository reviewRepository;

    @Override
    public Review save(Review review) {
        return toDomain(reviewRepository.save(toEntity(review)));
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
                .findByBookIdAndDeletedFalse(bookId, PageRequest.of(pageQuery.page(), pageQuery.size()))
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
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .deleted(review.isDeleted())
                .build();
    }

    private Review toDomain(ReviewJpaEntity entity) {
        return Review.reconstitute(
                entity.getId(),
                entity.getBookId(),
                entity.getCustomerId(),
                entity.getRating(),
                entity.getComment(),
                entity.getCreatedAt(),
                entity.isDeleted()
        );
    }
}
