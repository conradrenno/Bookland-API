package com.devrenno.bookland.catalog.application.dto;

import java.util.List;
import java.util.UUID;

/**
 * The answer to "reserve these units for this order".
 *
 * @param reserved           whether every line was reserved; never partially
 * @param unavailableBookIds the books that were short, when not reserved. Empty when the answer is
 *                           repeated from an earlier failure, which does not keep the list
 */
public record StockReservationResult(boolean reserved, List<UUID> unavailableBookIds) {

    public static StockReservationResult success() {
        return new StockReservationResult(true, List.of());
    }

    public static StockReservationResult failure(List<UUID> unavailableBookIds) {
        return new StockReservationResult(false, List.copyOf(unavailableBookIds));
    }
}
