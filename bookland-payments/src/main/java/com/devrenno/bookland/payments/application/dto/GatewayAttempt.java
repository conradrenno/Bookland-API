package com.devrenno.bookland.payments.application.dto;

import java.time.Instant;

/**
 * What one trip to the gateway came to. The application layer does not log; it returns this, and the
 * worker that drives it decides what deserves a log line — without it, a gateway outage was recorded
 * on the payments and seen by no one.
 *
 * @param attempts failed calls so far for the pending operation (meaningful for {@link Outcome#NO_ANSWER})
 * @param retryAt  when the next call is due (only for {@link Outcome#NO_ANSWER})
 * @param error    why the call failed, or why the refund was refused
 */
public record GatewayAttempt(Outcome outcome, int attempts, Instant retryAt, String error) {

    public enum Outcome {
        /** The payment was not waiting on the gateway (already settled, or gone). */
        NOTHING_PENDING,
        /** The gateway answered and the answer is recorded: approved, declined or refunded. */
        SETTLED,
        /** No answer: still pending, tried again at {@code retryAt}. */
        NO_ANSWER,
        /** The gateway refused the refund for good: REFUND_FAILED, a person has to settle it. */
        REFUND_REFUSED
    }

    public static GatewayAttempt nothingPending() {
        return new GatewayAttempt(Outcome.NOTHING_PENDING, 0, null, null);
    }

    public static GatewayAttempt settled() {
        return new GatewayAttempt(Outcome.SETTLED, 0, null, null);
    }

    public static GatewayAttempt noAnswer(int attempts, Instant retryAt, String error) {
        return new GatewayAttempt(Outcome.NO_ANSWER, attempts, retryAt, error);
    }

    public static GatewayAttempt refundRefused(String reason) {
        return new GatewayAttempt(Outcome.REFUND_REFUSED, 0, null, reason);
    }
}
