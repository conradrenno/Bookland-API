package com.devrenno.bookland.payments.infrastructure.messaging.outbox;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on {@code @Scheduled} for the payments module's relay and its gateway worker. The reviews module declares the same switch
 * for its own; declaring it here too is what keeps the relay running the day the payments module is a
 * service of its own, without reviews on its classpath. A second declaration is harmless.
 */
@Configuration
@EnableScheduling
public class PaymentsOutboxConfig {
}
