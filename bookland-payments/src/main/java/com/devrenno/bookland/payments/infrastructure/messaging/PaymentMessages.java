package com.devrenno.bookland.payments.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The payments module's side of the payment messages. The JSON is the contract, not these classes;
 * unknown fields are ignored so a sender can add one without breaking this consumer.
 */
final class PaymentMessages {

    private PaymentMessages() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChargePayment(UUID messageId, String type, UUID orderId, UUID customerId,
                         BigDecimal amount, String method) {
    }

    /** {@code PaymentApproved}, or {@code PaymentDeclined} with its reason. */
    record PaymentReply(UUID messageId, String type, UUID orderId, String reason) {
    }
}
