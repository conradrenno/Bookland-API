package com.devrenno.bookland.notification.infrastructure.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The topology the broker is asked to create. The tests of the whole service have no broker, so the
 * arguments that make the retry work — the TTLs and the dead-letter routes — are pinned here, and
 * their effect on a real RabbitMQ is measured end to end.
 */
class NotificationRabbitConfigTest {

    private final Declarables topology = new NotificationRabbitConfig()
            .notificationEmailTopology(new NotificationRetryProperties(null));

    @Test
    void theDefaultDelaysAreTenSecondsOneMinuteFiveMinutes() {
        assertThat(new NotificationRetryProperties(null).delays())
                .containsExactly(Duration.ofSeconds(10), Duration.ofMinutes(1), Duration.ofMinutes(5));
    }

    /** Rejected outright, a task follows the email queue's dead-letter route instead of vanishing. */
    @Test
    void theEmailQueueDeadLettersToTheDeadLetterQueue() {
        assertThat(queue("bookland.notification.email").getArguments()).containsAllEntriesOf(Map.of(
                "x-dead-letter-exchange", "bookland.notification",
                "x-dead-letter-routing-key", "dlq"));
        assertThat(boundWith("dlq")).isEqualTo("bookland.notification.email.dlq");
    }

    /** A waiting task goes back to the email queue when its queue's TTL runs out. */
    @Test
    void eachDelayIsAQueueWithThatTtlThatDeadLettersBackToTheEmailQueue() {
        for (String name : List.of("wait-10s", "wait-1m", "wait-5m")) {
            assertThat(boundWith(name)).isEqualTo("bookland.notification.email." + name);
        }
        assertThat(queue("bookland.notification.email.wait-10s").getArguments()).containsAllEntriesOf(Map.of(
                "x-message-ttl", 10_000,
                "x-dead-letter-exchange", "bookland.notification",
                "x-dead-letter-routing-key", "email"));
        assertThat(queue("bookland.notification.email.wait-5m").getArguments()).containsEntry("x-message-ttl", 300_000);
    }

    @Test
    void everyQueueIsDurable() {
        assertThat(topology.getDeclarablesByType(Queue.class)).allSatisfy(queue -> assertThat(queue.isDurable()).isTrue());
    }

    private Queue queue(String name) {
        return topology.getDeclarablesByType(Queue.class).stream()
                .filter(queue -> queue.getName().equals(name)).findFirst().orElseThrow();
    }

    private String boundWith(String routingKey) {
        return topology.getDeclarablesByType(Binding.class).stream()
                .filter(binding -> binding.getRoutingKey().equals(routingKey))
                .map(Binding::getDestination).findFirst().orElseThrow();
    }
}
