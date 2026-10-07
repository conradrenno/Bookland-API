package com.devrenno.bookland.payments.application.port.in;

import com.devrenno.bookland.payments.application.dto.ProcessPaymentCommand;

/**
 * Records that an order is to be charged. Nothing reaches the gateway here: the charge is made
 * afterwards by {@link ProcessPendingPaymentsUseCase}, and the saga's reply leaves when it is answered.
 */
public interface RequestChargeUseCase {

    /** Idempotent by order: a second request for an order already recorded changes nothing. */
    void requestCharge(ProcessPaymentCommand command);
}
