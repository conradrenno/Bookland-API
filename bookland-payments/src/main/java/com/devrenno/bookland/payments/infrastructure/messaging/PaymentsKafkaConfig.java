package com.devrenno.bookland.payments.infrastructure.messaging;

import com.devrenno.bookland.payments.domain.exception.PaymentNotFoundException;
import com.devrenno.bookland.payments.domain.exception.RefundNotAllowedException;
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
 * The payments module's messaging: the topics it owns and its own listener container factory.
 *
 * <p>Payments owns both topics of its interface — commands come in on one, replies go out on the
 * other — and knows nothing about who sends the commands.
 */
@Configuration
public class PaymentsKafkaConfig {

    static final String LISTENER_CONTAINER_FACTORY = "paymentsKafkaListenerContainerFactory";

    public static final String PAYMENT_COMMANDS_TOPIC = "bookland.payments.payment-commands";
    public static final String PAYMENT_REPLIES_TOPIC = "bookland.payments.payment-replies";

    public static final String CHARGE_PAYMENT = "ChargePayment";
    public static final String PAYMENT_APPROVED = "PaymentApproved";
    public static final String PAYMENT_DECLINED = "PaymentDeclined";

    /**
     * Owned by orders, consumed here: a cancelled order's payment is refunded. Written out rather
     * than imported, like any contract payments does not own.
     */
    public static final String ORDER_EVENTS_TOPIC = "bookland.orders.order-events";
    public static final String ORDER_CANCELLED = "OrderCancelled";

    @Bean
    public NewTopic paymentCommandsTopic() {
        return TopicBuilder.name(PAYMENT_COMMANDS_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentRepliesTopic() {
        return TopicBuilder.name(PAYMENT_REPLIES_TOPIC).partitions(3).replicas(1).build();
    }

    /**
     * Where a message this module could not apply ends up: {@code <topic>.payments.DLT}, one per topic
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
        return topic + ".payments.DLT";
    }

    private static NewTopic deadLetterTopicFor(String topic) {
        return TopicBuilder.name(deadLetterTopic(topic)).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentsPaymentCommandsDeadLetterTopic() {
        return deadLetterTopicFor(PAYMENT_COMMANDS_TOPIC);
    }

    @Bean
    public NewTopic paymentsOrderEventsDeadLetterTopic() {
        return deadLetterTopicFor(ORDER_EVENTS_TOPIC);
    }

    /**
     * Three more attempts one second apart, then the record goes to the payments dead-letter topic. A
     * payload that is not valid JSON, or a refund for an order that was never paid, fails the same way
     * every time, so it goes there straight away. The gateway is never called from a listener, so a
     * gateway outage cannot send a command here: it only delays the worker.
     */
    @Bean(LISTENER_CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<Object, Object> paymentsKafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> kafkaConsumerFactory,
            KafkaTemplate<String, String> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory);

        DeadLetterPublishingRecoverer deadLetters = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(deadLetterTopic(record.topic()), record.partition()));
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(deadLetters, new FixedBackOff(1000L, 3L));
        errorHandler.addNotRetryableExceptions(JacksonException.class, PaymentNotFoundException.class,
                RefundNotAllowedException.class);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
