package com.devrenno.bookland.notification.infrastructure.messaging;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * The notification service's RabbitMQ: the queue of emails waiting to be sent, the queues where a
 * failed one waits before its next try, and the dead-letter queue. RabbitMQ is here, and only here,
 * because this is a task queue and not a log of events — each email is taken by one consumer,
 * acknowledged when sent, and put aside to wait when it fails.
 *
 * <pre>
 *                          ┌──────────── "email" ───────────► bookland.notification.email ──► EmailTaskListener
 *                          │                                     │ rejected (dead-letter exchange, key "dlq")
 *  bookland.notification ──┤                                     ▼
 *     (direct exchange)    ├──────────── "dlq" ─────────────► bookland.notification.email.dlq   (nobody consumes)
 *                          │
 *                          ├── "wait-10s" ─► ….email.wait-10s ─┐
 *                          ├── "wait-1m"  ─► ….email.wait-1m  ─┼─ TTL expires: dead-lettered with key "email",
 *                          └── "wait-5m"  ─► ….email.wait-5m  ─┘  back to the email queue
 * </pre>
 *
 * <p>The wait queues have no consumer: a message sits there until the queue's TTL runs out, and
 * then the broker itself moves it — through the queue's dead-letter exchange — back to the email
 * queue. One queue per delay, with the TTL on the queue rather than on each message, because the
 * broker only expires messages at the head of a queue: a 5-minute message ahead of a 10-second one
 * in a single queue would hold the short one back for the whole 5 minutes.
 *
 * <p>The email queue's own dead-letter exchange catches what the listener rejects outright — a body
 * that is not a task, a retry that could not be scheduled — so nothing is ever dropped.
 *
 * <p>Everything is durable and the messages persistent: a task the broker confirmed survives its
 * restart. Declared here ({@link Declarables}) and created by Spring's {@code RabbitAdmin} when the
 * first connection opens. A queue's arguments cannot change once it exists: changing them means
 * deleting the queue on the broker first (in the compose stack, recreating the container).
 */
@Configuration
@EnableConfigurationProperties(NotificationRetryProperties.class)
public class NotificationRabbitConfig {

    static final String EXCHANGE = "bookland.notification";
    static final String EMAIL_QUEUE = "bookland.notification.email";
    static final String EMAIL_ROUTING_KEY = "email";
    static final String DEAD_LETTER_QUEUE = EMAIL_QUEUE + ".dlq";
    static final String DEAD_LETTER_ROUTING_KEY = "dlq";

    static final String LISTENER_CONTAINER_FACTORY = "notificationRabbitListenerContainerFactory";

    static String waitQueue(Duration delay) {
        return EMAIL_QUEUE + "." + NotificationRetryProperties.waitQueueSuffix(delay);
    }

    static String waitRoutingKey(Duration delay) {
        return NotificationRetryProperties.waitQueueSuffix(delay);
    }

    @Bean
    public Declarables notificationEmailTopology(NotificationRetryProperties retry) {
        DirectExchange exchange = ExchangeBuilder.directExchange(EXCHANGE).durable(true).build();
        Queue emailQueue = QueueBuilder.durable(EMAIL_QUEUE)
                .deadLetterExchange(EXCHANGE)
                .deadLetterRoutingKey(DEAD_LETTER_ROUTING_KEY)
                .build();
        Queue deadLetterQueue = QueueBuilder.durable(DEAD_LETTER_QUEUE).build();

        List<Declarable> declarables = new ArrayList<>(List.of(
                exchange, emailQueue, deadLetterQueue,
                BindingBuilder.bind(emailQueue).to(exchange).with(EMAIL_ROUTING_KEY),
                BindingBuilder.bind(deadLetterQueue).to(exchange).with(DEAD_LETTER_ROUTING_KEY)));
        for (Duration delay : retry.delays()) {
            Queue waitQueue = QueueBuilder.durable(waitQueue(delay))
                    .ttl((int) delay.toMillis())
                    .deadLetterExchange(EXCHANGE)
                    .deadLetterRoutingKey(EMAIL_ROUTING_KEY)
                    .build();
            declarables.add(waitQueue);
            declarables.add(BindingBuilder.bind(waitQueue).to(exchange).with(waitRoutingKey(delay)));
        }
        return new Declarables(declarables);
    }

    /**
     * This module's own factory, with its failure policy, like its Kafka one: a message the listener
     * throws on is rejected without going back to the email queue — the broker's default, requeue,
     * would hand it straight back, forever. Rejected, it follows the queue's dead-letter exchange to
     * the dead-letter queue. The ordinary failure, a mail server that does not answer, never gets
     * here: the listener schedules the retry itself and acknowledges.
     */
    @Bean(LISTENER_CONTAINER_FACTORY)
    public SimpleRabbitListenerContainerFactory notificationRabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
