package com.devrenno.bookland.catalog.infrastructure.messaging;

import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;
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

    /**
     * Owned by orders, consumed here: a cancelled order's reservation goes back to the shelf. Written
     * out rather than imported, like any contract the catalog does not own.
     */
    public static final String ORDER_EVENTS_TOPIC = "bookland.orders.order-events";
    public static final String ORDER_CANCELLED = "OrderCancelled";

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
     * Where a message this module could not apply ends up: {@code <topic>.catalog.DLT}, one per topic
     * it consumes, owned by this module because the failure is its own. Another module consuming the
     * same topic has its own dead letters, so reprocessing one never replays the message to the
     * other. A record keeps its partition, so each dead-letter topic has as many as its source; the
     * headers say which group failed it, why and from what offset ({@code kafka_dlt-*}).
     *
     * <p>Nothing reads these topics automatically: a person looks (Redpanda Console) and decides.
     * The bean names carry the module: the catalog's and payments' dead letters for order-events
     * would otherwise share a name, and the context refuses to start.
     */
    static String deadLetterTopic(String topic) {
        return topic + ".catalog.DLT";
    }

    private static NewTopic deadLetterTopicFor(String topic) {
        return TopicBuilder.name(deadLetterTopic(topic)).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic catalogBookRatingChangedDeadLetterTopic() {
        return deadLetterTopicFor(BookRatingChangedListener.TOPIC);
    }

    @Bean
    public NewTopic catalogStockCommandsDeadLetterTopic() {
        return deadLetterTopicFor(STOCK_COMMANDS_TOPIC);
    }

    @Bean
    public NewTopic catalogOrderEventsDeadLetterTopic() {
        return deadLetterTopicFor(ORDER_EVENTS_TOPIC);
    }

    /**
     * Three more attempts one second apart, then the record goes to the catalog's dead-letter topic,
     * so one bad message cannot stall its partition and is not lost either. A payload that is not
     * valid JSON, or a book that does not exist, fails the same way every time: those go there
     * straight away.
     */
    @Bean(LISTENER_CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<Object, Object> catalogKafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> kafkaConsumerFactory,
            KafkaTemplate<String, String> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory);

        DeadLetterPublishingRecoverer deadLetters = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(deadLetterTopic(record.topic()), record.partition()));
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(deadLetters, new FixedBackOff(1000L, 3L));
        errorHandler.addNotRetryableExceptions(JacksonException.class, BookNotFoundException.class);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
