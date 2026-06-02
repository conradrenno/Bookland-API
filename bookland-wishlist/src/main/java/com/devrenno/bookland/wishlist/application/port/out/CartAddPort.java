package com.devrenno.bookland.wishlist.application.port.out;

import java.util.UUID;

public interface CartAddPort {
    void addToCart(UUID customerId, UUID bookId, int quantity);
}
