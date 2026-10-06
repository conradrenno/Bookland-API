package com.devrenno.bookland.orders.application.dto;

import java.util.List;
import java.util.UUID;

/**
 * What the catalog answered to "reserve the units of this order".
 *
 * @param reserved           every line reserved; the catalog never reserves partially
 * @param unavailableBookIds the books that were short, when not reserved
 */
public record StockReservationOutcome(boolean reserved, List<UUID> unavailableBookIds) {
}
