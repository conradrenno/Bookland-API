package com.devrenno.bookland.reviews.infrastructure.persistence.repository;

import com.devrenno.bookland.reviews.infrastructure.persistence.entity.ReviewJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewJpaRepository extends JpaRepository<ReviewJpaEntity, UUID> {
    Page<ReviewJpaEntity> findByBookIdAndDeletedFalse(UUID bookId, Pageable pageable);
    Optional<ReviewJpaEntity> findByBookIdAndCustomerIdAndDeletedFalse(UUID bookId, UUID customerId);
    List<ReviewJpaEntity> findAllByBookIdAndDeletedFalse(UUID bookId);
}
