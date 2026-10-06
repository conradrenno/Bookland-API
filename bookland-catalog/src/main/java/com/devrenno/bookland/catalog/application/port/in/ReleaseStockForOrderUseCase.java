package com.devrenno.bookland.catalog.application.port.in;

import java.util.UUID;

/**
 * Gives back the units reserved for an order — the compensation when its payment is declined, and
 * the stock half of a cancellation.
 *
 * <p>Idempotent: releasing twice, or releasing an order that never reserved anything, does nothing.
 */
public interface ReleaseStockForOrderUseCase {

    void release(UUID orderId);
}
