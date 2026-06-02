package com.devrenno.bookland.wishlist.infrastructure.persistence.repository;

import com.devrenno.bookland.wishlist.infrastructure.persistence.entity.WishlistJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WishlistJpaRepository extends JpaRepository<WishlistJpaEntity, UUID> {
    Optional<WishlistJpaEntity> findByCustomerId(UUID customerId);
}
