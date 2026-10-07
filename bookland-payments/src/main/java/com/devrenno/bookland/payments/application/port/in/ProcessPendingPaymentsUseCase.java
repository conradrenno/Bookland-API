package com.devrenno.bookland.payments.application.port.in;

import java.util.List;
import java.util.UUID;

/**
 * The gateway worker's two halves: which payments are due at the gateway now, and taking one of them
 * there. Split so the caller (a scheduler) can go through them one by one and carry on past a failure.
 */
public interface ProcessPendingPaymentsUseCase {

    /** The pending charges and refunds whose next attempt is due, oldest first. */
    List<UUID> findDue(int limit);

    /**
     * Calls the gateway for one payment and records the answer: approved, declined, refunded, refund
     * refused — or, if no answer came, when to try again. Does nothing for a payment no longer pending.
     */
    void process(UUID paymentId);
}
