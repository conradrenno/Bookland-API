package com.devrenno.bookland.orders.infrastructure.persistence.repository;

import com.devrenno.bookland.orders.infrastructure.persistence.entity.CartJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartJpaRepository extends JpaRepository<CartJpaEntity, UUID> {
    Optional<CartJpaEntity> findByCustomerId(UUID customerId);
    void deleteByCustomerId(UUID customerId);
}
