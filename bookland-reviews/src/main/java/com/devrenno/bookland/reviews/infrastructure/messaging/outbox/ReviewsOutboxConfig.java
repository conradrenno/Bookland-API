package com.devrenno.bookland.reviews.infrastructure.messaging.outbox;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on {@code @Scheduled}, which nothing else in the application uses yet: without it the relay
 * is an ordinary bean that never runs, the outbox fills up, and no event ever reaches Kafka — with no
 * error anywhere. The switch is application-wide; it lives here because the relay is what needs it.
 */
@Configuration
@EnableScheduling
public class ReviewsOutboxConfig {
}
