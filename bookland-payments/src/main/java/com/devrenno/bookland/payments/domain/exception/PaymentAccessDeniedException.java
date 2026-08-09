package com.devrenno.bookland.payments.domain.exception;

import java.util.UUID;

/**
 * A caller reached the payment of an order that is not theirs. A business 403, not a role problem —
 * see docs/error-contract.md.
 */
public class PaymentAccessDeniedException extends RuntimeException {

    public PaymentAccessDeniedException(UUID orderId) {
        super("Access denied to the payment of order: " + orderId);
    }
}
