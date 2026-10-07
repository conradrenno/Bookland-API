package com.devrenno.bookland.payments.infrastructure.persistence.repository;

import com.devrenno.bookland.payments.infrastructure.persistence.entity.PaymentJpaEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, UUID> {
    Optional<PaymentJpaEntity> findByOrderId(UUID orderId);

    List<PaymentJpaEntity> findByStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            Collection<String> statuses, Instant now, Limit limit);
}
