package com.devrenno.bookland;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The notification service: emails the customer about their orders. It reads the order events from
 * Kafka, queues one task per email on RabbitMQ and sends it over SMTP. No HTTP API — no web server
 * is on the classpath, and the process stays up because its Kafka and RabbitMQ listeners do.
 *
 * <p>Lives in the root package {@code com.devrenno.bookland} on purpose: component, entity and
 * repository scanning start here and reach the notification module's packages — moving it into a
 * subpackage would silently drop them.
 */
@SpringBootApplication
public class NotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}
