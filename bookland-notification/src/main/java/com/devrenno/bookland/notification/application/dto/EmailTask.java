package com.devrenno.bookland.notification.application.dto;

/**
 * One email to send, as it travels on the task queue: already written, so whoever takes it from
 * the queue only has to deliver it.
 *
 * @param key the order and the kind of email ({@code <orderId>:<KIND>}) — one email per order and
 *            kind, whatever number of times the task is delivered
 */
public record EmailTask(String key, String to, String subject, String body) {
}
