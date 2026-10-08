package com.devrenno.bookland.notification.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The notification service's RabbitMQ: the queue of emails waiting to be sent. RabbitMQ is here, and
 * only here, because this is a task queue and not a log of events — each email is taken by one
 * consumer, acknowledged when sent, and (step 6c) put back to wait before another try.
 *
 * <pre>
 *  publisher ──► exchange bookland.notification (direct) ──email──► queue bookland.notification.email ──► EmailTaskListener
 * </pre>
 *
 * <p>Exchange and queue are durable and the messages persistent: a task confirmed by the broker
 * survives a broker restart. They are declared here ({@link Declarables}), and Spring's
 * {@code RabbitAdmin} creates them on the broker when the first connection opens.
 */
@Configuration
public class NotificationRabbitConfig {

    static final String EXCHANGE = "bookland.notification";
    static final String EMAIL_QUEUE = "bookland.notification.email";
    static final String EMAIL_ROUTING_KEY = "email";

    static final String LISTENER_CONTAINER_FACTORY = "notificationRabbitListenerContainerFactory";

    @Bean
    public Declarables notificationEmailTopology() {
        DirectExchange exchange = ExchangeBuilder.directExchange(EXCHANGE).durable(true).build();
        Queue queue = QueueBuilder.durable(EMAIL_QUEUE).build();
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(EMAIL_ROUTING_KEY);
        return new Declarables(exchange, queue, binding);
    }

    /**
     * This module's own factory, with its failure policy, like its Kafka one: a task that fails is
     * rejected without going back to the queue. The broker's default — requeue — would hand the
     * same task back at once, forever, while the mail server is down. For now a rejected task is
     * dropped (and logged); step 6c gives it a delayed retry and a dead-letter queue.
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
