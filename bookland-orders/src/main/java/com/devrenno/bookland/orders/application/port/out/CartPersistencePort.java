package com.devrenno.bookland.orders.application.port.out;

import com.devrenno.bookland.orders.domain.entity.Cart;

import java.util.Optional;
import java.util.UUID;

public interface CartPersistencePort {
    Cart save(Cart cart);
    Optional<Cart> findByCustomerId(UUID customerId);
    void deleteByCustomerId(UUID customerId);
}
