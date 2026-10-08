package com.devrenno.bookland.notification.infrastructure.messaging;

import com.devrenno.bookland.notification.application.dto.EmailTask;
import com.devrenno.bookland.notification.application.port.out.EmailTaskQueuePort;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Puts an email task on the queue and waits for the broker to say it has it — a publisher confirm.
 * Without the wait, {@code send} returns as soon as the bytes leave this process: a broker that
 * dies right then loses the task, and the Kafka offset would already say the event was handled.
 *
 * <p>Needs {@code spring.rabbitmq.publisher-confirm-type: simple}: the channel then counts what it
 * published, and {@code waitForConfirmsOrDie} blocks until the broker acknowledges all of it, or
 * throws on a refusal (nack) or when the time runs out.
 *
 * <p>A confirm says the exchange took the message, not that a queue did: a message that matches no
 * binding is confirmed and dropped. The binding is declared by this service itself, so that would
 * take someone deleting it on the broker.
 */
@Component
public class RabbitEmailTaskQueueAdapter implements EmailTaskQueuePort {

    private static final Duration CONFIRM_TIMEOUT = Duration.ofSeconds(5);

    private final RabbitTemplate rabbitTemplate;
    private final EmailTaskCodec codec;

    public RabbitEmailTaskQueueAdapter(RabbitTemplate rabbitTemplate, EmailTaskCodec codec) {
        this.rabbitTemplate = rabbitTemplate;
        this.codec = codec;
    }

    @Override
    public void enqueue(EmailTask task) {
        rabbitTemplate.invoke(operations -> {
            operations.send(NotificationRabbitConfig.EXCHANGE, NotificationRabbitConfig.EMAIL_ROUTING_KEY,
                    codec.toMessage(task));
            operations.waitForConfirmsOrDie(CONFIRM_TIMEOUT.toMillis());
            return null;
        });
    }
}
