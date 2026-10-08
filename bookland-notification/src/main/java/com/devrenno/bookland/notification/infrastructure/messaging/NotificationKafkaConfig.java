package com.devrenno.bookland.notification.infrastructure.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
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
     * Handling an event here fails for one of two reasons. A payload that is not valid JSON fails
     * the same way every time, so it goes to the dead-letter topic at once. Anything else is the task
     * queue not confirming — RabbitMQ down or unreachable — which is worth waiting out: the wait
     * doubles from 1 s to 30 s, for about five minutes in all, before the event is set aside in the
     * dead-letter topic.
     *
     * <p>Those retries hold the partition: events behind this one wait. That is deliberate — with
     * the queue down none of them could be handled either — and it keeps one order's events in the
     * order they happened. The longest single wait (30 s) stays well under the consumer's
     * {@code max.poll.interval.ms} (5 min by default).
     */
    @Bean(LISTENER_CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<Object, Object> notificationKafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> kafkaConsumerFactory,
            KafkaTemplate<String, String> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory);

        DeadLetterPublishingRecoverer deadLetters = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(deadLetterTopic(record.topic()), record.partition()));
        ExponentialBackOff backOff = new ExponentialBackOff(Duration.ofSeconds(1).toMillis(), 2.0);
        backOff.setMaxInterval(Duration.ofSeconds(30).toMillis());
        backOff.setMaxElapsedTime(Duration.ofMinutes(5).toMillis());
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(deadLetters, backOff);
        errorHandler.addNotRetryableExceptions(JacksonException.class);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
