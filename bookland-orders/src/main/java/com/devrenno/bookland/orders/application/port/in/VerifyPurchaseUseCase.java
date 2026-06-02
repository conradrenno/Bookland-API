package com.devrenno.bookland.orders.application.port.in;

import java.util.UUID;

public interface VerifyPurchaseUseCase {
    boolean hasDeliveredOrderWithBook(UUID customerId, UUID bookId);
}
