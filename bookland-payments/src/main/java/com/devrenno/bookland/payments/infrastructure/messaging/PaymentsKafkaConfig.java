package com.devrenno.bookland.payments.infrastructure.messaging;

import com.devrenno.bookland.payments.domain.exception.PaymentNotFoundException;
import com.devrenno.bookland.payments.domain.exception.RefundNotAllowedException;
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
     * Three more attempts one second apart, then the record is logged and skipped. A payload that is
     * not valid JSON, or a refund for an order that was never paid, fails the same way every time, so
     * it skips straight to the log. No dead-letter topic yet.
     */
    @Bean(LISTENER_CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<Object, Object> paymentsKafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> kafkaConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(new FixedBackOff(1000L, 3L));
        errorHandler.addNotRetryableExceptions(JacksonException.class, PaymentNotFoundException.class,
                RefundNotAllowedException.class);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
