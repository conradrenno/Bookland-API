package com.devrenno.bookland.orders.application.port.out;

import java.util.UUID;

/**
 * Query-side port: checks whether a customer has a delivered order containing a given book.
 * Implemented by the persistence adapter (was previously an inner-layer dependency on the JPA
 * repository — an architecture violation fixed by this port).
 */
public interface PurchaseVerificationPort {

    boolean existsDeliveredOrderWithBook(UUID customerId, UUID bookId);
}
