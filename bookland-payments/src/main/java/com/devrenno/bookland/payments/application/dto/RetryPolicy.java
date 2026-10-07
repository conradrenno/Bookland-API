package com.devrenno.bookland.payments.application.dto;

import java.time.Duration;

/**
 * How long to wait before calling the gateway again: doubling from {@code initialDelay} after each
 * failed attempt, never more than {@code maxDelay}. There is no last attempt — with an idempotency key
 * a retry cannot move money twice, and giving up on a charge whose outcome is unknown could leave a
 * customer charged for an order that failed.
 */
public record RetryPolicy(Duration initialDelay, Duration maxDelay) {

    /** The wait after the {@code failedAttempts}-th failure (1 → initialDelay, 2 → twice that, …). */
    public Duration delayAfter(int failedAttempts) {
        int doublings = Math.min(Math.max(failedAttempts - 1, 0), 30);
        Duration delay = initialDelay.multipliedBy(1L << doublings);
        return delay.compareTo(maxDelay) > 0 ? maxDelay : delay;
    }
}
