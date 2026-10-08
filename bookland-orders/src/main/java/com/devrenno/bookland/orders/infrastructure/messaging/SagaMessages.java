package com.devrenno.bookland.orders.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The orders module's side of its messages: the saga commands it writes, the replies it reads and
 * the events it announces.
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

    /**
     * Every event on the order-events topic has this shape; {@code type} says which one it is. It
     * carries the order as it stands after the change, so a consumer needs nothing else: the catalog
     * and payments read only {@code orderId}, the notification service the customer and the items.
     * {@code customerEmail} and {@code customerName} are null for orders placed before they were
     * stored; {@code reason} is set for REJECTED and PAYMENT_FAILED.
     */
    record OrderEvent(UUID messageId, String type, UUID orderId, UUID customerId, String customerEmail,
                      String customerName, String status, String reason, BigDecimal totalAmount,
                      List<OrderEventItem> items, Instant occurredAt) {
    }

    record OrderEventItem(UUID bookId, String title, int quantity, BigDecimal unitPrice) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StockReply(UUID messageId, String type, UUID orderId, List<UUID> unavailableBookIds) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PaymentReply(UUID messageId, String type, UUID orderId, String reason) {
    }
}
