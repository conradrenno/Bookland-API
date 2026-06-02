package com.devrenno.bookland.orders.infrastructure.persistence.repository;

import com.devrenno.bookland.orders.infrastructure.persistence.entity.OrderJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {
    Page<OrderJpaEntity> findByCustomerId(UUID customerId, Pageable pageable);
}
