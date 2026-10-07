package com.devrenno.bookland.orders.infrastructure.messaging;

import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.core.JacksonException;

/**
 * The orders module's messaging for the checkout saga.
 *
 * <p>Orders owns none of these four topics: each belongs to the service whose interface it is — the
 * stock topics to the catalog, the payment topics to payments. Their names are the contract and are
 * written out here on purpose rather than imported: importing them would make the orders module
 * depend on the other two, which is the coupling the messages exist to remove.
 */
@Configuration
public class OrdersKafkaConfig {

    static final String LISTENER_CONTAINER_FACTORY = "ordersKafkaListenerContainerFactory";

    public static final String STOCK_COMMANDS_TOPIC = "bookland.catalog.stock-commands";
    public static final String STOCK_REPLIES_TOPIC = "bookland.catalog.stock-replies";
    public static final String PAYMENT_COMMANDS_TOPIC = "bookland.payments.payment-commands";
    public static final String PAYMENT_REPLIES_TOPIC = "bookland.payments.payment-replies";

    public static final String RESERVE_STOCK = "ReserveStock";
    public static final String RELEASE_STOCK = "ReleaseStock";
    public static final String CHARGE_PAYMENT = "ChargePayment";

    static final String STOCK_RESERVED = "StockReserved";
    static final String STOCK_RESERVATION_FAILED = "StockReservationFailed";
    static final String PAYMENT_APPROVED = "PaymentApproved";
    static final String PAYMENT_DECLINED = "PaymentDeclined";

    /**
     * Three more attempts one second apart, then the record is logged and skipped. A payload that is
     * not valid JSON fails the same way every time, so it skips straight to the log.
     */
    @Bean(LISTENER_CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<Object, Object> ordersKafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> kafkaConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(new FixedBackOff(1000L, 3L));
        errorHandler.addNotRetryableExceptions(JacksonException.class);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
