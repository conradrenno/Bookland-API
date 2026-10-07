package com.devrenno.bookland.orders.application.port.out;

import com.devrenno.bookland.orders.domain.entity.Cart;

import java.util.Optional;
import java.util.UUID;

public interface CartPersistencePort {
    Cart save(Cart cart);
    Optional<Cart> findByCustomerId(UUID customerId);
    void deleteByCustomerId(UUID customerId);

    /**
     * Claims the customer's cart for a checkout, atomically: true when no other checkout held it.
     * Two simultaneous checkouts both reading "no checkout in progress" and both going ahead is
     * exactly what an atomic claim prevents and a read-then-write cannot.
     */
    boolean claimForCheckout(UUID customerId, UUID orderId);

    /** Lets the next checkout claim the cart; the saga calls it when an order fails. */
    void releaseCheckoutClaim(UUID customerId);
}
