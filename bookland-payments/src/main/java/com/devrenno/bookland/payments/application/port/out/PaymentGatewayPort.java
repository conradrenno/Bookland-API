package com.devrenno.bookland.payments.application.port.out;

import com.devrenno.bookland.payments.application.dto.PaymentResult;
import com.devrenno.bookland.payments.application.dto.ProcessPaymentCommand;

import java.util.UUID;

/**
 * The payment provider.
 *
 * <p>Every call carries an <b>idempotency key</b>: the same key sent again gets the first answer back
 * and moves no money a second time. That is what makes it safe to call again when the first call's
 * outcome is unknown — a timeout, or a crash before the answer was saved. Real providers offer it
 * (an {@code Idempotency-Key} header, typically); without it, every retry risks a double charge.
 *
 * <p>A call that gets no answer throws {@link PaymentGatewayUnavailableException}; the caller tries
 * again later with the same key.
 */
public interface PaymentGatewayPort {

    /** Approved or declined. Throws {@link PaymentGatewayUnavailableException} when there was no answer. */
    PaymentResult charge(String idempotencyKey, ProcessPaymentCommand command);

    /**
     * Gives the charge back. Throws {@link RefundRejectedException} when the provider refuses for good,
     * {@link PaymentGatewayUnavailableException} when there was no answer. The order id travels as
     * metadata, as providers allow.
     */
    void refund(String idempotencyKey, UUID orderId, String transactionId);
}
