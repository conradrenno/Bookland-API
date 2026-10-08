package com.devrenno.bookland.notification.infrastructure.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.amqp.AmqpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.ExponentialBackOff;
import tools.jackson.core.JacksonException;

import java.time.Duration;

/**
 * The notification service's side of Kafka: it consumes the order events and owns no topic but its
 * own dead letters. The topic's name is written out, not imported: it belongs to orders, and the
 * name is the contract.
 */
@Configuration
public class NotificationKafkaConfig {

    static final String LISTENER_CONTAINER_FACTORY = "notificationKafkaListenerContainerFactory";

    /** Owned by orders; keyed by the order id. */
    static final String ORDER_EVENTS_TOPIC = "bookland.orders.order-events";

    static final String CONSUMER_GROUP = "bookland-notification";

    /** {@code <topic>.notification.DLT}: this service's dead letters, apart from the other consumers'. */
    static String deadLetterTopic(String topic) {
        return topic + ".notification.DLT";
    }

    @Bean
    public NewTopic notificationOrderEventsDeadLetterTopic() {
        return TopicBuilder.name(deadLetterTopic(ORDER_EVENTS_TOPIC)).partitions(3).replicas(1).build();
    }

    /**
     * Three kinds of failure, three answers:
     *
     * <ul>
     *   <li><b>A payload that is not valid JSON</b> fails the same way every time: dead-letter topic
     *       at once.</li>
     *   <li><b>RabbitMQ not taking the task</b> — any {@link AmqpException} in the cause chain: down,
     *       unreachable, no confirm — is waited out <b>with no last attempt</b>, the wait doubling up to
     *       {@code max-interval}. Giving up would not help: the next event would find the same broker
     *       down, and each one given up is an email nobody sends unless a person replays the
     *       dead-letter topic. Measured in 6d: with a 5-minute limit, a longer outage sent every event
     *       of the outage there.</li>
     *   <li><b>Anything else</b> — a bug, not an outage — waits the same way until the waits add up
     *       to {@code max-elapsed} (5 min) and then goes to the dead-letter topic, so one bad event
     *       cannot stall the partition forever. The limit is the sum of the waits Spring's
     *       {@code ExponentialBackOff} hands out, not the clock (checked in its bytecode): each try's
     *       own duration comes on top — 345 s in all, measured in 6d.</li>
     * </ul>
     *
     * <p>Which of the two applies is decided by the event's first failure (the error handler asks
     * the back-off function once per record, checked in its bytecode).
     *
     * <p>Those retries hold the partition: events behind this one wait. That is the price, and only
     * this service pays it — the catalog and payments read the same topic under their own groups. It
     * also keeps one order's events in the order they happened. The longest single wait stays well
     * under the consumer's {@code max.poll.interval.ms} (5 min by default).
     */
    @Bean(LISTENER_CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<Object, Object> notificationKafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> kafkaConsumerFactory,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${bookland.notification.kafka-retry.initial-interval:1s}") Duration initialInterval,
            @Value("${bookland.notification.kafka-retry.max-interval:30s}") Duration maxInterval,
            @Value("${bookland.notification.kafka-retry.max-elapsed:5m}") Duration maxElapsed) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory);

        DeadLetterPublishingRecoverer deadLetters = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(deadLetterTopic(record.topic()), record.partition()));
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(deadLetters,
                backOff(initialInterval, maxInterval, maxElapsed));
        errorHandler.setBackOffFunction((record, exception) -> isBrokerUnavailable(exception)
                ? backOff(initialInterval, maxInterval, null)
                : null);
        errorHandler.addNotRetryableExceptions(JacksonException.class);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    /** {@code maxElapsed} null: no limit — retried until it works. */
    private static BackOff backOff(Duration initialInterval, Duration maxInterval, Duration maxElapsed) {
        ExponentialBackOff backOff = new ExponentialBackOff(initialInterval.toMillis(), 2.0);
        backOff.setMaxInterval(maxInterval.toMillis());
        backOff.setMaxElapsedTime(maxElapsed == null ? Long.MAX_VALUE : maxElapsed.toMillis());
        return backOff;
    }

    static boolean isBrokerUnavailable(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof AmqpException) {
                return true;
            }
        }
        return false;
    }
}
