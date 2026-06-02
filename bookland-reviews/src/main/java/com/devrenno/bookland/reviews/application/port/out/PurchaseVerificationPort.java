package com.devrenno.bookland.reviews.application.port.out;

import java.util.UUID;

public interface PurchaseVerificationPort {
    boolean hasPurchasedBook(UUID customerId, UUID bookId);
}
