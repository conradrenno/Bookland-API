package com.devrenno.bookland;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Every full-context test of the notification service: the dev profile, an in-JVM Kafka broker (the
 * service consumes the order events), and {@link NotificationTestDoubles} in place of RabbitMQ and
 * the mail server, which have no embedded version (decision 5 of step 6). The RabbitMQ listener
 * container does not start, so nothing tries to reach a broker; the real queue's behaviour —
 * confirms, persistence, redelivery — is exercised against a running RabbitMQ in the experiments.
 * The Kafka side's retry waits are shrunk (milliseconds instead of seconds) so their limits can be
 * crossed in a test.
 *
 * <p>One annotation keeps the whole suite on a single cached context and a single broker.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@ActiveProfiles("dev")
@EmbeddedKafka(bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@TestPropertySource(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        // The Kafka side's waits, scaled down so a test can outlast the 5-minute limit in a second.
        "bookland.notification.kafka-retry.initial-interval=10ms",
        "bookland.notification.kafka-retry.max-interval=50ms",
        "bookland.notification.kafka-retry.max-elapsed=1s"})
@Import(NotificationTestDoubles.class)
public @interface NotificationIntegrationTest {
}
