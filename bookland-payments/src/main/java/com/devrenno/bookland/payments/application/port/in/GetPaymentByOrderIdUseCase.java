package com.devrenno.bookland.payments.application.port.in;

import com.devrenno.bookland.payments.domain.entity.Payment;

import java.util.UUID;

/**
 * Reads the payment of an order on behalf of a caller, who may only read their own.
 *
 * <p>The requester is a parameter rather than something the web layer checks afterwards: a payment
 * carries the amount, the method and the gateway transaction id, so an unowned read is a disclosure
 * whatever the caller does with the result. Keeping the rule here means it holds for every consumer
 * of this operation, not just for the one HTTP route that exists today.
 */
public interface GetPaymentByOrderIdUseCase {

    Payment getByOrderId(UUID orderId, UUID requesterId);
}
