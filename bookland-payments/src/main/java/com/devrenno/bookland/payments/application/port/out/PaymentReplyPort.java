package com.devrenno.bookland.payments.application.port.out;

import java.util.UUID;

/**
 * The checkout saga's answer to a charge command, sent once the gateway has answered. Implemented by
 * writing to the payments outbox in the caller's transaction — the one that records the answer — so
 * the payment's outcome and the reply announcing it commit together.
 */
public interface PaymentReplyPort {

    void paymentApproved(UUID orderId);

    void paymentDeclined(UUID orderId, String reason);
}
