package com.devrenno.bookland.orders.infrastructure.persistence.repository;

import com.devrenno.bookland.orders.infrastructure.persistence.entity.CartJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CartJpaRepository extends JpaRepository<CartJpaEntity, UUID> {
    Optional<CartJpaEntity> findByCustomerId(UUID customerId);
    void deleteByCustomerId(UUID customerId);

    /**
     * The claim: one statement that both checks and writes, so two checkouts cannot both see the cart
     * free. Returns the number of rows changed — 1 when the claim was taken, 0 when another holds it.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update CartJpaEntity c
               set c.checkoutOrderId = :orderId
             where c.customerId = :customerId
               and c.checkoutOrderId is null
            """)
    int claimForCheckout(@Param("customerId") UUID customerId, @Param("orderId") UUID orderId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update CartJpaEntity c set c.checkoutOrderId = null where c.customerId = :customerId")
    int releaseCheckoutClaim(@Param("customerId") UUID customerId);
}
