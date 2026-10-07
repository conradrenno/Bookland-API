package com.devrenno.bookland.orders.infrastructure.messaging;

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
 * The orders module's messaging: the checkout saga and the order's own events.
 *
 * <p>Orders owns none of the four saga topics: each belongs to the service whose interface it is —
 * the stock topics to the catalog, the payment topics to payments. Their names are the contract and
 * are written out here on purpose rather than imported: importing them would make the orders module
 * depend on the other two, which is the coupling the messages exist to remove.
 *
 * <p>It does own {@link #ORDER_EVENTS_TOPIC}: an event belongs to whoever it happened to, and its
 * consumers write the name out the same way.
 */
@Configuration
public class OrdersKafkaConfig {

    static final String LISTENER_CONTAINER_FACTORY = "ordersKafkaListenerContainerFactory";

    public static final String STOCK_COMMANDS_TOPIC = "bookland.catalog.stock-commands";
    public static final String STOCK_REPLIES_TOPIC = "bookland.catalog.stock-replies";
    public static final String PAYMENT_COMMANDS_TOPIC = "bookland.payments.payment-commands";
    public static final String PAYMENT_REPLIES_TOPIC = "bookland.payments.payment-replies";

    /** What happened to orders, for whoever cares; keyed by the order id. */
    public static final String ORDER_EVENTS_TOPIC = "bookland.orders.order-events";

    public static final String RESERVE_STOCK = "ReserveStock";
    public static final String RELEASE_STOCK = "ReleaseStock";
    public static final String CHARGE_PAYMENT = "ChargePayment";
    public static final String ORDER_CANCELLED = "OrderCancelled";

    static final String STOCK_RESERVED = "StockReserved";
    static final String STOCK_RESERVATION_FAILED = "StockReservationFailed";
    static final String PAYMENT_APPROVED = "PaymentApproved";
    static final String PAYMENT_DECLINED = "PaymentDeclined";

    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(ORDER_EVENTS_TOPIC).partitions(3).replicas(1).build();
    }

    /**
     * Where a message this module could not apply ends up: {@code <topic>.orders.DLT}, one per topic
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
        return topic + ".orders.DLT";
    }

    private static NewTopic deadLetterTopicFor(String topic) {
        return TopicBuilder.name(deadLetterTopic(topic)).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic ordersStockRepliesDeadLetterTopic() {
        return deadLetterTopicFor(STOCK_REPLIES_TOPIC);
    }

    @Bean
    public NewTopic ordersPaymentRepliesDeadLetterTopic() {
        return deadLetterTopicFor(PAYMENT_REPLIES_TOPIC);
    }

    /**
     * Three more attempts one second apart, then the record goes to the orders dead-letter topic. A
     * payload that is not valid JSON fails the same way every time, so it goes there straight away.
     */
    @Bean(LISTENER_CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<Object, Object> ordersKafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> kafkaConsumerFactory,
            KafkaTemplate<String, String> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory);

        DeadLetterPublishingRecoverer deadLetters = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(deadLetterTopic(record.topic()), record.partition()));
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(deadLetters, new FixedBackOff(1000L, 3L));
        errorHandler.addNotRetryableExceptions(JacksonException.class);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
