package com.devrenno.bookland.payments.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * An event from the orders topic, as payments reads it: only the fields it needs. {@code type} says
 * which event it is; payments acts on {@code OrderCancelled} and ignores the rest.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record OrderEventMessage(UUID messageId, String type, UUID orderId) {
}
