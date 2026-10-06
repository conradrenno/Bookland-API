package com.devrenno.bookland.catalog.application.port.in;

import com.devrenno.bookland.catalog.application.dto.StockReservationResult;
import com.devrenno.bookland.catalog.domain.entity.StockReservation;

import java.util.List;
import java.util.UUID;

/**
 * Takes the units an order needs, all of them or none — the first step of the checkout saga.
 *
 * <p>Idempotent by order id: asking again for an order that was already answered returns the same
 * outcome and changes nothing.
 */
public interface ReserveStockForOrderUseCase {

    StockReservationResult reserve(UUID orderId, List<StockReservation.Item> items);
}
