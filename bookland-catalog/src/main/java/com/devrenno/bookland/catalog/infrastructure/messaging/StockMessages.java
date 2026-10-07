package com.devrenno.bookland.catalog.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.UUID;

/**
 * The catalog's side of the stock messages. What the catalog and its callers share is the JSON, not
 * these classes: whoever sends a command writes its own record with the same fields. Unknown fields
 * are ignored so a sender can add one without breaking the catalog.
 */
final class StockMessages {

    private StockMessages() {}

    /**
     * A command as it arrives. One shape for both kinds — {@code type} says which — since
     * {@code ReleaseStock} is {@code ReserveStock} without the items.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record StockCommand(UUID messageId, String type, UUID orderId, List<Line> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Line(UUID bookId, int quantity) {
    }

    /** The reply to a {@code ReserveStock}: {@code StockReserved} or {@code StockReservationFailed}. */
    record StockReply(UUID messageId, String type, UUID orderId, List<UUID> unavailableBookIds) {
    }
}
