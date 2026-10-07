package com.devrenno.bookland.orders.application.port.in;

import java.util.List;
import java.util.UUID;

/**
 * The orchestrator's reactions to the replies of the checkout saga: each moves the order one step and
 * says what comes next.
 *
 * <p>A reply that arrives for an order not in the state that expects it — a duplicate, or a reply to
 * a step already past — is ignored. That, together with the inbox, is what makes the orchestrator
 * idempotent. Each method answers whether the reply was applied, so the adapter that received it can
 * log an ignored one.
 */
public interface CheckoutSagaUseCase {

    boolean onStockReserved(UUID orderId);

    boolean onStockReservationFailed(UUID orderId, List<UUID> unavailableBookIds);

    boolean onPaymentApproved(UUID orderId);

    boolean onPaymentDeclined(UUID orderId, String reason);
}
