package com.devrenno.bookland.payments.application.port.in;

import java.util.UUID;

/**
 * Records that an order's payment is to be given back. As with the charge, the gateway is reached
 * afterwards, by {@link ProcessPendingPaymentsUseCase}.
 */
public interface RequestRefundUseCase {

    /** Idempotent: a refund already requested, done or refused is left alone. */
    void requestRefund(UUID orderId);
}
