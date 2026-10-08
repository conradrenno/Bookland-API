package com.devrenno.bookland.notification.application.port.out;

/**
 * The emails already sent, by key ({@code <orderId>:<KIND>}). What makes sending idempotent: a
 * task can reach the sender more than once — the same event queued twice after a crash, a retry
 * whose first try did go through — and only the first one may become an email.
 */
public interface SentEmailPort {

    boolean wasSent(String key);

    void recordSent(String key, String recipient, String subject);
}
