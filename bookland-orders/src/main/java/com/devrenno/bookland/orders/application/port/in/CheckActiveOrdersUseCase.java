package com.devrenno.bookland.orders.application.port.in;

import java.util.UUID;

/**
 * Cross-module boundary: tells whether a book appears in any order still in progress.
 * Consumed by the catalog module (a book with active orders cannot be removed).
 */
public interface CheckActiveOrdersUseCase {
    boolean hasActiveOrdersForBook(UUID bookId);
}
