package com.devrenno.bookland.payments.application.port.out;

/**
 * The gateway gave no answer — down, timed out, connection dropped. The outcome is unknown: the money
 * may or may not have moved. Never read as a decline; the call is repeated with the same idempotency key.
 */
public class PaymentGatewayUnavailableException extends RuntimeException {
    public PaymentGatewayUnavailableException(String message) {
        super(message);
    }
}
