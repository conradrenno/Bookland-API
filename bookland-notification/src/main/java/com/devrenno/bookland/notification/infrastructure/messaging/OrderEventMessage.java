package com.devrenno.bookland.notification.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * An event from the orders topic, as the notification service reads it: only the fields an email
 * needs. Orders writes more (customerId, status, occurredAt…); they are ignored, as is any field
 * added later. What is shared with orders is this JSON, not a class.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record OrderEventMessage(UUID messageId, String type, UUID orderId, String customerEmail, String customerName,
                         String reason, BigDecimal totalAmount, List<Item> items) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Item(String title, int quantity, BigDecimal unitPrice) {
    }
}
