package com.devrenno.bookland.orders.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The orders module's side of the saga messages: the commands it writes and the replies it reads.
 * What it shares with the catalog and payments is the JSON, not these classes — each of them keeps
 * its own records with the same fields.
 */
final class SagaMessages {

    private SagaMessages() {}

    record ReserveStock(UUID messageId, String type, UUID orderId, List<Line> items) {
    }

    record Line(UUID bookId, int quantity) {
    }

    record ReleaseStock(UUID messageId, String type, UUID orderId) {
    }

    record ChargePayment(UUID messageId, String type, UUID orderId, UUID customerId,
                         BigDecimal amount, String method) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StockReply(UUID messageId, String type, UUID orderId, List<UUID> unavailableBookIds) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PaymentReply(UUID messageId, String type, UUID orderId, String reason) {
    }
}
