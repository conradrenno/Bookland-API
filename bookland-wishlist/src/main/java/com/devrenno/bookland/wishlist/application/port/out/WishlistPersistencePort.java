package com.devrenno.bookland.wishlist.application.port.out;

import com.devrenno.bookland.wishlist.domain.entity.Wishlist;

import java.util.Optional;
import java.util.UUID;

public interface WishlistPersistencePort {
    Wishlist save(Wishlist wishlist);
    Optional<Wishlist> findByCustomerId(UUID customerId);
}
