package com.devrenno.bookland.orders.application.port.out;

import java.util.function.Supplier;

/**
 * Framework-free unit-of-work boundary. Runs the given work inside a single transaction.
 * Implemented in infrastructure (Spring's TransactionTemplate). Keeps @Transactional out of the
 * framework-free inner layers, and works under manual wiring (no Spring proxy required).
 */
public interface TransactionPort {

    void inTransaction(Runnable work);

    <T> T inTransaction(Supplier<T> work);
}
