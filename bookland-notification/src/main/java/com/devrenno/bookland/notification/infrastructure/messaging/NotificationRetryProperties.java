package com.devrenno.bookland.notification.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * How long a failed email waits before each new try ({@code bookland.notification.retry.delays}):
 * one entry per retry, so the default — 10 s, 1 min, 5 min — is the first try plus three more, after
 * which the task goes to the dead-letter queue.
 *
 * <p>Each delay is a queue of its own on the broker, named after it ({@link #waitQueueSuffix}): a
 * queue's arguments cannot change once declared, so a different delay is a different queue rather
 * than a refused redeclaration.
 */
@ConfigurationProperties("bookland.notification.retry")
public record NotificationRetryProperties(List<Duration> delays) {

    public NotificationRetryProperties {
        delays = delays == null
                ? List.of(Duration.ofSeconds(10), Duration.ofMinutes(1), Duration.ofMinutes(5))
                : List.copyOf(delays);
    }

    /** {@code wait-10s}, {@code wait-1m}: the delay, readable in the broker's UI. */
    static String waitQueueSuffix(Duration delay) {
        long seconds = delay.toSeconds();
        return seconds % 60 == 0 ? "wait-" + seconds / 60 + "m" : "wait-" + seconds + "s";
    }
}
