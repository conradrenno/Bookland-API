package com.devrenno.bookland;

import com.devrenno.bookland.payments.infrastructure.gateway.SimulatedPaymentGatewayAdapter;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * What protects the money when things go wrong: the gateway not answering, the gateway refusing a
 * refund, and a message no consumer can apply.
 *
 * <p>The simulated gateway is told to fail for one customer or one order only, so these tests do not
 * disturb whatever else the shared context is running. The worker's retry waits start at one second
 * and double, so three failures cost about seven seconds before the answer.
 */
@BooklandIntegrationTest
class PaymentSafetyIntegrationTest {

    private static final Duration WAIT = Duration.ofSeconds(40);

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired private EmbeddedKafkaBroker broker;
    @Autowired private SimulatedPaymentGatewayAdapter gateway;
    @Autowired private JWKSource<SecurityContext> jwkSource;

    @Value("${bookland.resource-server.issuer}")
    private String issuer;

    @Value("${bookland.resource-server.audience}")
    private String apiAudience;

    private final ObjectMapper json = new ObjectMapper();

    private TestAccessTokens tokens;
    private UUID bookId;

    @BeforeEach
    void setUp() {
        tokens = new TestAccessTokens(jwkSource, issuer, apiAudience);
        bookId = jdbcTemplate.queryForObject(
                "select id from books where active = true and price >= 34.00 order by isbn limit 1", UUID.class);
        jdbcTemplate.update("update books set stock_quantity = 100 where id = ?", bookId);
    }

    /**
     * Before: three quick retries by the listener container, then the command skipped — the order
     * stuck in AWAITING_PAYMENT for good. Now the charge waits in the table and completes when the
     * gateway answers.
     */
    @Test
    @DisplayName("gateway down for a charge: the payment waits PENDING, then the order is CONFIRMED")
    void chargeSurvivesAGatewayOutage() throws Exception {
        UUID customerId = UUID.randomUUID();
        gateway.failNextCharges(customerId, 3);
        String token = tokens.forCaller(customerId, "CUSTOMER");

        UUID orderId = checkout(token);

        await().atMost(WAIT).untilAsserted(() -> {
            assertThat(orderStatus(orderId)).isEqualTo("AWAITING_PAYMENT");
            assertThat(paymentColumn(orderId, "status")).isEqualTo("PENDING");
            assertThat(paymentColumn(orderId, "last_error")).contains("Simulated outage");
        });
        await().atMost(WAIT).untilAsserted(() -> assertThat(orderStatus(orderId)).isEqualTo("CONFIRMED"));
        assertThat(paymentColumn(orderId, "status")).isEqualTo("APPROVED");
        assertThat(paymentColumn(orderId, "attempts")).isEqualTo("3");
    }

    @Test
    @DisplayName("gateway down for a refund: REFUND_PENDING, then REFUNDED")
    void refundSurvivesAGatewayOutage() throws Exception {
        String token = tokens.forCaller(UUID.randomUUID(), "CUSTOMER");
        UUID orderId = confirmedOrder(token);
        gateway.failNextRefunds(orderId, 2);

        cancel(token, orderId);

        await().atMost(WAIT).untilAsserted(() -> assertThat(paymentColumn(orderId, "status")).isEqualTo("REFUNDED"));
        assertThat(paymentColumn(orderId, "attempts")).isEqualTo("2");
        assertThat(stock()).isEqualTo(100);
    }

    /** The money did not go back: the state says so, for a person to act on — not a line in a log. */
    @Test
    @DisplayName("refund refused by the gateway: REFUND_FAILED with the reason, order stays CANCELLED")
    void refusedRefundIsVisible() throws Exception {
        String token = tokens.forCaller(UUID.randomUUID(), "CUSTOMER");
        UUID orderId = confirmedOrder(token);
        gateway.refuseRefunds(orderId);

        cancel(token, orderId);

        await().atMost(WAIT).untilAsserted(() ->
                assertThat(paymentColumn(orderId, "status")).isEqualTo("REFUND_FAILED"));
        assertThat(paymentColumn(orderId, "last_error")).contains("refund window closed");
        assertThat(paymentColumn(orderId, "next_attempt_at")).isNull();
        assertThat(orderStatus(orderId)).isEqualTo("CANCELLED");
        assertThat(stock()).as("the stock half of the cancellation is unaffected").isEqualTo(100);
    }

    /**
     * A message that cannot be read goes to the consuming module's dead-letter topic, kept, instead of
     * being skipped with a log line. One per module that consumes, and the shared order-events topic
     * has one per consumer: catalog and payments each keep their own copy.
     */
    @Test
    @DisplayName("an unreadable message lands in the dead-letter topic of every module that consumes it")
    void unreadableMessagesAreKept() {
        Map<String, List<String>> deadLettersBySource = Map.of(
                "bookland.payments.payment-commands", List.of("bookland.payments.payment-commands.payments.DLT"),
                "bookland.catalog.stock-commands", List.of("bookland.catalog.stock-commands.catalog.DLT"),
                "bookland.catalog.stock-replies", List.of("bookland.catalog.stock-replies.orders.DLT"),
                "bookland.orders.order-events", List.of("bookland.orders.order-events.catalog.DLT",
                        "bookland.orders.order-events.payments.DLT"));

        deadLettersBySource.forEach((source, deadLetterTopics) -> {
            String key = UUID.randomUUID().toString();
            kafkaTemplate.send(source, key, "this is not json");
            for (String deadLetterTopic : deadLetterTopics) {
                ConsumerRecord<String, String> kept = awaitRecord(deadLetterTopic, key);
                assertThat(kept.value()).isEqualTo("this is not json");
                assertThat(header(kept, "kafka_dlt-original-topic")).isEqualTo(source);
            }
        });
    }

    /** Not only unreadable: a well-formed event payments cannot apply is kept too, with its cause. */
    @Test
    @DisplayName("a cancellation for an order payments never charged lands in the payments dead-letter topic")
    void inapplicableEventIsKept() {
        UUID orderId = UUID.randomUUID();
        kafkaTemplate.send("bookland.orders.order-events", orderId.toString(), """
                {"messageId":"%s","type":"OrderCancelled","orderId":"%s"}
                """.formatted(UUID.randomUUID(), orderId));

        ConsumerRecord<String, String> kept = awaitRecord("bookland.orders.order-events.payments.DLT", orderId.toString());

        assertThat(header(kept, "kafka_dlt-exception-fqcn") + " " + header(kept, "kafka_dlt-exception-cause-fqcn"))
                .contains("PaymentNotFoundException");
    }

    // --- helpers ---

    private UUID confirmedOrder(String token) throws Exception {
        UUID orderId = checkout(token);
        await().atMost(WAIT).untilAsserted(() -> assertThat(orderStatus(orderId)).isEqualTo("CONFIRMED"));
        return orderId;
    }

    private UUID checkout(String token) throws Exception {
        mockMvc.perform(post("/api/v1/cart/items").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": "%s", "quantity": 1}
                                """.formatted(bookId)))
                .andExpect(status().is2xxSuccessful());
        String body = mockMvc.perform(post("/api/v1/cart/checkout").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentMethod": "CREDIT_CARD"}
                                """))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(body).get("id").asText());
    }

    private void cancel(String token, UUID orderId) throws Exception {
        mockMvc.perform(delete("/api/v1/orders/{id}", orderId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String orderStatus(UUID orderId) {
        return jdbcTemplate.queryForObject("select status from orders where id = ?", String.class, orderId);
    }

    private String paymentColumn(UUID orderId, String column) {
        List<String> found = jdbcTemplate.queryForList(
                "select cast(" + column + " as varchar(500)) from payments where order_id = ?", String.class, orderId);
        return found.isEmpty() ? null : found.get(0);
    }

    private int stock() {
        return jdbcTemplate.queryForObject("select stock_quantity from books where id = ?", Integer.class, bookId);
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

    private static String header(ConsumerRecord<String, String> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
