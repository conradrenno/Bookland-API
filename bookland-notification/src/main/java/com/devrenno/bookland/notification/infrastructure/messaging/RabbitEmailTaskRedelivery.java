package com.devrenno.bookland.notification.infrastructure.messaging;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Puts a failed task on the wait queue for its delay, or on the dead-letter queue, as a copy marked
 * with the next try and the last error — and waits for the broker's confirm, like the first publish.
 *
 * <p>The listener acknowledges the original only after this returns. A crash in between leaves both:
 * the original goes back to the email queue (it was never acknowledged) and the copy waits. One of
 * them sends the email; the other finds it in {@code sent_emails}.
 */
@Component
public class RabbitEmailTaskRedelivery implements EmailTaskRedelivery {

    private static final Duration CONFIRM_TIMEOUT = Duration.ofSeconds(5);

    private final RabbitTemplate rabbitTemplate;
    private final EmailTaskCodec codec;

    public RabbitEmailTaskRedelivery(RabbitTemplate rabbitTemplate, EmailTaskCodec codec) {
        this.rabbitTemplate = rabbitTemplate;
        this.codec = codec;
    }

    @Override
    public void retryLater(Message failed, int nextAttempt, Duration delay, String reason) {
        publish(NotificationRabbitConfig.waitRoutingKey(delay), codec.withAttempt(failed, nextAttempt, reason));
    }

    @Override
    public void giveUp(Message failed, int attempts, String reason) {
        publish(NotificationRabbitConfig.DEAD_LETTER_ROUTING_KEY, codec.withAttempt(failed, attempts, reason));
    }

    private void publish(String routingKey, Message message) {
        rabbitTemplate.invoke(operations -> {
            operations.send(NotificationRabbitConfig.EXCHANGE, routingKey, message);
            operations.waitForConfirmsOrDie(CONFIRM_TIMEOUT.toMillis());
            return null;
        });
    }
}
