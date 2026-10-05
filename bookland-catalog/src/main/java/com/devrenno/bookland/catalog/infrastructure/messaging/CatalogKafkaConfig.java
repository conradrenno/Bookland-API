package com.devrenno.bookland.catalog.infrastructure.messaging;

import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;
import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
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
