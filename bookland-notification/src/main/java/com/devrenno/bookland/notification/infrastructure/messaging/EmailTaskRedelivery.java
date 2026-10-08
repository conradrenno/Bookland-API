package com.devrenno.bookland.notification.infrastructure.messaging;

import org.springframework.amqp.core.Message;

import java.time.Duration;

/**
 * Where a task that failed goes next: to wait before another try, or to the dead-letter queue.
 * Each method returns only once the broker has the message, and throws otherwise — the listener then
 * lets the original be rejected, and the email queue's dead-letter exchange keeps it.
 *
 * <p>An interface, not just the RabbitMQ class, so the tests (which have no broker) can stand in
 * for the waiting.
 */
public interface EmailTaskRedelivery {

    void retryLater(Message failed, int nextAttempt, Duration delay, String reason);

    void giveUp(Message failed, int attempts, String reason);
}
