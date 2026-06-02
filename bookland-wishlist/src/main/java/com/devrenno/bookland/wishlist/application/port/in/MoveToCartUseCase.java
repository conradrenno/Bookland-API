package com.devrenno.bookland.wishlist.application.port.in;

import java.util.UUID;

public interface MoveToCartUseCase {
    void execute(UUID customerId, UUID bookId);
}
