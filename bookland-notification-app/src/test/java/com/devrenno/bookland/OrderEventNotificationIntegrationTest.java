package com.devrenno.bookland;

import com.devrenno.bookland.NotificationTestDoubles.InMemoryEmailQueue;
import com.devrenno.bookland.NotificationTestDoubles.InMemoryRedelivery;
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
 * The order events as orders publishes them, in through Kafka, out as emails — including what
 * happens when the mail server fails: the waits between tries, the dead-letter queue, and the
 * duplicate task that only {@code sent_emails} can catch. The events are written
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
    @Autowired private InMemoryRedelivery redelivery;
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

    /**
     * Measured in 6d: with a time limit on every failure, a RabbitMQ outage longer than the limit sent
     * each event of the outage to the dead-letter topic, and those emails were never sent. The broker
     * being unreachable is now waited out with no last attempt — here well past the (scaled-down)
     * limit that still applies to other failures.
     */
    @Test
    @DisplayName("RabbitMQ unreachable past the time limit: still waited out, the email is sent, nothing dead-lettered")
    void brokerOutageHasNoTimeLimit() {
        String to = uniqueAddress();
        UUID orderId = UUID.randomUUID();
        // The limit counts the waits handed out (10, 20, 40, then 50 ms each), not the clock: 30
        // refusals add up to ~1.4 s of waiting, past the 1 s that dead-letters any other failure.
        // Each retry round trip takes ~0.5 s here (seen in the test log), hence the longer await.
        queue.refuseNext(30);

        publish(event(UUID.randomUUID(), "OrderConfirmed", orderId, to));

        await().atMost(Duration.ofSeconds(60)).untilAsserted(() -> assertThat(mailSender.sentTo(to)).hasSize(1));
        assertThat(deadLetterRecord(orderId.toString(), Duration.ofSeconds(2))).isNull();
    }

    /** Anything that is not the broker — a bug — keeps the limit, so one bad event cannot stall the partition. */
    @Test
    @DisplayName("any other failure past the time limit: the event goes to the dead-letter topic, no email")
    void otherFailuresKeepTheTimeLimit() {
        String to = uniqueAddress();
        UUID orderId = UUID.randomUUID();
        queue.failNextWithBug(1000);

        publish(event(UUID.randomUUID(), "OrderConfirmed", orderId, to));

        ConsumerRecord<String, String> kept = awaitRecord(TOPIC + ".notification.DLT", orderId.toString());
        queue.failNextWithBug(0);
        assertThat(kept.value()).contains(orderId.toString());
        assertThat(mailSender.sentTo(to)).isEmpty();
    }

    @Test
    @DisplayName("a payload that is not JSON goes to this service's dead-letter topic")
    void unreadableEventGoesToTheDeadLetterTopic() {
        String key = UUID.randomUUID().toString();

        kafkaTemplate.send(TOPIC, key, "not json at all");

        ConsumerRecord<String, String> kept = awaitRecord(TOPIC + ".notification.DLT", key);
        assertThat(kept.value()).isEqualTo("not json at all");
    }

    @Test
    @DisplayName("the mail server fails twice: the task waits 10 s, then 1 min, and the third try sends it")
    void mailServerOutageIsRetried() {
        String to = uniqueAddress();
        UUID orderId = UUID.randomUUID();
        mailSender.failNext(to, 2);

        publish(event(UUID.randomUUID(), "OrderConfirmed", orderId, to));

        awaitOneEmailTo(to);
        assertThat(redelivery.waitsOf(orderId + ":CONFIRMED"))
                .containsExactly(Duration.ofSeconds(10), Duration.ofMinutes(1));
        assertThat(redelivery.deadLettersOf(orderId + ":CONFIRMED")).isEmpty();
        assertThat(sentEmailRecorded(orderId + ":CONFIRMED")).isTrue();
    }

    @Test
    @DisplayName("the mail server never answers: four tries, three waits, then the dead-letter queue — nothing recorded as sent")
    void mailServerThatNeverAnswersEndsInTheDeadLetterQueue() {
        String to = uniqueAddress();
        UUID orderId = UUID.randomUUID();
        String key = orderId + ":CONFIRMED";
        mailSender.failNext(to, 100);

        publish(event(UUID.randomUUID(), "OrderConfirmed", orderId, to));

        await().atMost(WAIT).untilAsserted(() -> assertThat(redelivery.deadLettersOf(key)).hasSize(1));
        assertThat(redelivery.waitsOf(key))
                .containsExactly(Duration.ofSeconds(10), Duration.ofMinutes(1), Duration.ofMinutes(5));
        Map<String, Object> headers = redelivery.deadLettersOf(key).get(0).getMessageProperties().getHeaders();
        assertThat(headers).containsEntry("x-bookland-attempt", 4);
        assertThat((String) headers.get("x-bookland-last-error")).contains("mail server unreachable");
        assertThat(mailSender.sentTo(to)).isEmpty();
        assertThat(sentEmailRecorded(key)).isFalse();
    }

    /**
     * Two events with different message ids for the same order and kind get past the inbox — the case
     * of a task queued, then the process crashing before the inbox committed. The email's key is what
     * stops the second one.
     */
    @Test
    @DisplayName("the same email queued twice: sent once, the second task dropped by sent_emails")
    void duplicateTaskIsSentOnce() {
        String to = uniqueAddress();
        UUID orderId = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        publish(event(UUID.randomUUID(), "OrderConfirmed", orderId, to));
        publish(event(second, "OrderConfirmed", orderId, to));

        await().atMost(WAIT).untilAsserted(() -> assertThat(inboxHas(second)).isTrue());
        assertThat(mailSender.sentTo(to)).hasSize(1);
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

    private boolean sentEmailRecorded(String key) {
        return jdbcTemplate.queryForObject(
                "select count(*) from sent_emails where email_key = ?", Integer.class, key) == 1;
    }

    private boolean inboxHas(UUID messageId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from notification_inbox where message_id = ?", Integer.class, messageId) == 1;
    }

    private ConsumerRecord<String, String> awaitRecord(String topic, String key) {
        ConsumerRecord<String, String> found = deadLetterRecordIn(topic, key, WAIT);
        return found != null ? found : fail("No record with key " + key + " in " + topic);
    }

    /** The order's record in this service's dead-letter topic, or null if none shows up within {@code wait}. */
    private ConsumerRecord<String, String> deadLetterRecord(String key, Duration wait) {
        return deadLetterRecordIn(TOPIC + ".notification.DLT", key, wait);
    }

    private ConsumerRecord<String, String> deadLetterRecordIn(String topic, String key, Duration wait) {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString(),
                ConsumerConfig.GROUP_ID_CONFIG, "dead-letter-reader-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (Consumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            Instant deadline = Instant.now().plus(wait);
            while (Instant.now().isBefore(deadline)) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    if (key.equals(record.key())) {
                        return record;
                    }
                }
            }
        }
        return null;
    }
}
