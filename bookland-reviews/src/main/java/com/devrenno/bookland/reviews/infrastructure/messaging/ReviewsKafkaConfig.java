package com.devrenno.bookland.reviews.infrastructure.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Topics the reviews module produces to. The producer owns its topics, so they are declared here;
 * Spring's KafkaAdmin creates them on startup if they do not exist yet.
 */
@Configuration
public class ReviewsKafkaConfig {

    public static final String BOOK_RATING_CHANGED_TOPIC = "bookland.reviews.book-rating-changed";

    /**
     * Three partitions so different books are consumed in parallel; the message key (the book id)
     * keeps every event of one book in one partition, which is what preserves their order. One
     * replica because the compose stack runs a single broker.
     */
    @Bean
    public NewTopic bookRatingChangedTopic() {
        return TopicBuilder.name(BOOK_RATING_CHANGED_TOPIC).partitions(3).replicas(1).build();
    }
}
