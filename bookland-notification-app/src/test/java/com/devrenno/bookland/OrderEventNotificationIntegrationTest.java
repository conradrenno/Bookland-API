package com.devrenno.bookland;

import com.devrenno.bookland.NotificationTestDoubles.InMemoryEmailQueue;
import com.devrenno.bookland.NotificationTestDoubles.RecordingMailSender;
import com.devrenno.bookland.notification.domain.valueobject.EmailMessage;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

/**
 * The order events as orders publishes them, in through Kafka, out as emails. The events are written
 * by hand here with every field orders writes ({@code OrderEventsIntegrationTest} in bookland-app pins
 * that side), including the ones this service ignores.
 */
@NotificationIntegrationTest
class OrderEventNotificationIntegrationTest {

    private static final String TOPIC = "bookland.orders.order-events";
    private static final Duration WAIT = Duration.ofSeconds(20);

    @Autowired private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired private RecordingMailSender mailSender;
    @Autowired private InMemoryEmailQueue queue;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EmbeddedKafkaBroker broker;

    @Test
    @DisplayName("OrderConfirmed: one email to the address on the order, with the items and the total")
    void confirmedOrderIsEmailed() {
        String to = uniqueAddress();
        UUID orderId = UUID.randomUUID();

        publish(event(UUID.randomUUID(), "OrderConfirmed", orderId, to));

        EmailMessage email = awaitOneEmailTo(to);
        assertThat(email.subject()).contains("confirmed").contains(orderId.toString().substring(0, 8));
        assertThat(email.body())
                .startsWith("Hello Ana Reader,")
                .contains("2 x Clean Code — 40.00")
                .contains("Total: 80.00");
    }

    @Test
    @DisplayName("each kind the customer is told about becomes its own email")
    void everyKindIsEmailed() {
        String to = uniqueAddress();
        UUID orderId = UUID.randomUUID();

        for (String type : List.of("OrderConfirmed", "OrderShipped", "OrderCancelled")) {
            publish(event(UUID.randomUUID(), type, orderId, to));
        }

        await().atMost(WAIT).untilAsserted(() -> assertThat(mailSender.sentTo(to)).hasSize(3));
        assertThat(mailSender.sentTo(to)).extracting(EmailMessage::subject)
                .anySatisfy(subject -> assertThat(subject).contains("confirmed"))
                .anySatisfy(subject -> assertThat(subject).contains("on its way"))
                .anySatisfy(subject -> assertThat(subject).contains("cancelled"));
    }

    /** At least once: the orders relay resends a row it sent but could not stamp. */
    @Test
    @DisplayName("the same event delivered twice: one email")
    void redeliveredEventIsEmailedOnce() {
        String to = uniqueAddress();
        UUID orderId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID last = UUID.randomUUID();

        publish(event(messageId, "OrderConfirmed", orderId, to));
        publish(event(messageId, "OrderConfirmed", orderId, to));
        publish(event(last, "OrderShipped", orderId, to));

        // One key, one partition: once the last one is emailed, the duplicate before it was consumed.
        await().atMost(WAIT).untilAsserted(() -> assertThat(mailSender.sentTo(to))
                .anySatisfy(email -> assertThat(email.subject()).contains("on its way")));
        assertThat(mailSender.sentTo(to)).hasSize(2);
    }

    @Test
    @DisplayName("an order with no email on record, or an event no email is written for: nothing sent")
    void nothingToSend() {
        String to = uniqueAddress();
        UUID orderId = UUID.randomUUID();
        UUID noAddress = UUID.randomUUID();

        publish(event(noAddress, "OrderConfirmed", orderId, null));
        publish(event(UUID.randomUUID(), "SomethingElseHappened", orderId, to));
        publish(event(UUID.randomUUID(), "OrderShipped", orderId, to));

        await().atMost(WAIT).untilAsserted(() -> assertThat(mailSender.sentTo(to)).hasSize(1));
        assertThat(mailSender.sentTo(to).get(0).subject()).contains("on its way");
        assertThat(inboxHas(noAddress)).isTrue();
    }

    /**
     * The queue down for a moment: the event is not lost and not emailed twice. The inbox record
     * rolls back with the failed attempt, and the retry handles it for real.
     */
    @Test
    @DisplayName("the task queue refuses twice, then takes it: one email, after the retries")
    void queueOutageIsWaitedOut() {
        String to = uniqueAddress();
        UUID messageId = UUID.randomUUID();
        queue.refuseNext(2);

        publish(event(messageId, "OrderConfirmed", UUID.randomUUID(), to));

        awaitOneEmailTo(to);
        assertThat(inboxHas(messageId)).isTrue();
    }

    @Test
    @DisplayName("a payload that is not JSON goes to this service's dead-letter topic")
    void unreadableEventGoesToTheDeadLetterTopic() {
        String key = UUID.randomUUID().toString();

        kafkaTemplate.send(TOPIC, key, "not json at all");

        ConsumerRecord<String, String> kept = awaitRecord(TOPIC + ".notification.DLT", key);
        assertThat(kept.value()).isEqualTo("not json at all");
    }

    private EmailMessage awaitOneEmailTo(String to) {
        await().atMost(WAIT).untilAsserted(() -> assertThat(mailSender.sentTo(to)).hasSize(1));
        return mailSender.sentTo(to).get(0);
    }

    private void publish(String event) {
        String orderId = event.replaceAll("(?s).*\"orderId\":\"([^\"]+)\".*", "$1");
        kafkaTemplate.send(TOPIC, orderId, event);
    }

    /** Shaped as orders writes it ({@code SagaMessages.OrderEvent}), the fields this service ignores included. */
    private static String event(UUID messageId, String type, UUID orderId, String customerEmail) {
        return """
                {"messageId":"%s","type":"%s","orderId":"%s","customerId":"%s","customerEmail":%s,
                 "customerName":"Ana Reader","status":"CONFIRMED","reason":null,"totalAmount":80.00,
                 "items":[{"bookId":"%s","title":"Clean Code","quantity":2,"unitPrice":40.00}],
                 "occurredAt":"2026-10-08T12:00:00Z"}
                """.formatted(messageId, type, orderId, UUID.randomUUID(),
                customerEmail == null ? "null" : "\"" + customerEmail + "\"", UUID.randomUUID());
    }

    private static String uniqueAddress() {
        return "reader-" + UUID.randomUUID() + "@bookland.com";
    }

    private boolean inboxHas(UUID messageId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from notification_inbox where message_id = ?", Integer.class, messageId) == 1;
    }

    private ConsumerRecord<String, String> awaitRecord(String topic, String key) {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString(),
                ConsumerConfig.GROUP_ID_CONFIG, "dead-letter-reader-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (Consumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            Instant deadline = Instant.now().plus(WAIT);
            while (Instant.now().isBefore(deadline)) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    if (key.equals(record.key())) {
                        return record;
                    }
                }
            }
        }
        return fail("No record with key " + key + " in " + topic);
    }
}
