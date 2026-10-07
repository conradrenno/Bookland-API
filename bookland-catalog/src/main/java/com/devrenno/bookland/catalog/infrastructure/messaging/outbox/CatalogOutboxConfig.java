package com.devrenno.bookland.catalog.infrastructure.messaging.outbox;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on {@code @Scheduled} for the catalog's relay. The reviews module declares the same switch
 * for its own; declaring it here too is what keeps the relay running the day the catalog is a
 * service of its own, without reviews on its classpath. A second declaration is harmless.
 */
@Configuration
@EnableScheduling
public class CatalogOutboxConfig {
}
