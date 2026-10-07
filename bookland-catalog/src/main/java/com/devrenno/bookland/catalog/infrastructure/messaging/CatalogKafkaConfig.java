package com.devrenno.bookland.catalog.infrastructure.messaging;

import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.core.JacksonException;

/**
 * The catalog's own listener container factory, so its error policy applies to its listeners only.
 * A global {@code CommonErrorHandler} bean would reach every module's listeners — and a second one
 * declared by another module would make Boot apply neither, silently.
 */
@Configuration
public class CatalogKafkaConfig {

    static final String LISTENER_CONTAINER_FACTORY = "catalogKafkaListenerContainerFactory";

    /**
     * The catalog's stock interface for the checkout saga. The catalog owns both topics — the owner of
     * a command topic is whoever receives the commands, as a server owns its API — so it offers
     * "send stock commands here, replies come out there" and knows nothing about who asks.
     */
    public static final String STOCK_COMMANDS_TOPIC = "bookland.catalog.stock-commands";
    public static final String STOCK_REPLIES_TOPIC = "bookland.catalog.stock-replies";

    public static final String RESERVE_STOCK = "ReserveStock";
    public static final String RELEASE_STOCK = "ReleaseStock";
    public static final String STOCK_RESERVED = "StockReserved";
    public static final String STOCK_RESERVATION_FAILED = "StockReservationFailed";

    /** Three partitions; the key (the order id) keeps each order's messages in one, in order. */
    @Bean
    public NewTopic stockCommandsTopic() {
        return TopicBuilder.name(STOCK_COMMANDS_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic stockRepliesTopic() {
        return TopicBuilder.name(STOCK_REPLIES_TOPIC).partitions(3).replicas(1).build();
    }

    /**
     * Three more attempts one second apart, then the record is logged and skipped so one bad message
     * cannot stall its partition. A payload that is not valid JSON, or a book that does not exist,
     * will fail the same way every time: those skip straight to the log. No dead-letter topic yet.
     */
    @Bean(LISTENER_CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<Object, Object> catalogKafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> kafkaConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(new FixedBackOff(1000L, 3L));
        errorHandler.addNotRetryableExceptions(JacksonException.class, BookNotFoundException.class);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
