package com.devrenno.bookland;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The catalog's side of every Kafka conversation, on its own: commands and events arrive as JSON the
 * test writes, replies are read back from the broker. In the monolith these paths were only exercised
 * together with orders; here the catalog stands alone, as it now runs — the JSON is the contract,
 * whoever sends it.
 */
@CatalogIntegrationTest
class CatalogMessagingIntegrationTest {

    private static final Duration WAIT = Duration.ofSeconds(30);
    private static final String STOCK_COMMANDS = "bookland.catalog.stock-commands";
    private static final String STOCK_REPLIES = "bookland.catalog.stock-replies";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired private EmbeddedKafkaBroker broker;
    @Autowired private JWKSource<SecurityContext> jwkSource;

    @Value("${bookland.resource-server.issuer}")
    private String issuer;

    @Value("${bookland.resource-server.audience}")
    private String apiAudience;

    private final ObjectMapper json = new ObjectMapper();
    private TestAccessTokens tokens;

    @BeforeEach
    void setUp() {
        tokens = new TestAccessTokens(jwkSource, issuer, apiAudience);
    }

    @Test
    @DisplayName("ReserveStock with enough stock: units taken, StockReserved on the replies topic")
    void reserve() throws Exception {
        UUID bookId = newBook(10);
        UUID orderId = UUID.randomUUID();

        kafkaTemplate.send(STOCK_COMMANDS, orderId.toString(), reserve(UUID.randomUUID(), orderId, bookId, 3));

        JsonNode reply = awaitReply(orderId);
        assertThat(reply.get("type").asText()).isEqualTo("StockReserved");
        assertThat(stockOf(bookId)).isEqualTo(7);
        assertThat(reservationStatus(orderId)).isEqualTo("RESERVED");
    }

    @Test
    @DisplayName("ReserveStock beyond the stock: nothing taken, StockReservationFailed naming the book")
    void reserveFails() throws Exception {
        UUID bookId = newBook(2);
        UUID orderId = UUID.randomUUID();

        kafkaTemplate.send(STOCK_COMMANDS, orderId.toString(), reserve(UUID.randomUUID(), orderId, bookId, 5));

        JsonNode reply = awaitReply(orderId);
        assertThat(reply.get("type").asText()).isEqualTo("StockReservationFailed");
        assertThat(reply.get("unavailableBookIds").get(0).asText()).isEqualTo(bookId.toString());
        assertThat(stockOf(bookId)).isEqualTo(2);
    }

    /**
     * At-least-once delivery, on purpose: the same ReserveStock twice, then a ReleaseStock on the same
     * key. One partition keeps the three in order, so once the release is applied the duplicate has
     * been consumed too — and the stock moved down once and back once, with a single reply.
     */
    @Test
    @DisplayName("a duplicated ReserveStock is applied once; ReleaseStock gives the units back")
    void duplicateAndRelease() throws Exception {
        UUID bookId = newBook(10);
        UUID orderId = UUID.randomUUID();
        String command = reserve(UUID.randomUUID(), orderId, bookId, 3);

        kafkaTemplate.send(STOCK_COMMANDS, orderId.toString(), command);
        kafkaTemplate.send(STOCK_COMMANDS, orderId.toString(), command);
        kafkaTemplate.send(STOCK_COMMANDS, orderId.toString(), """
                {"messageId":"%s","type":"ReleaseStock","orderId":"%s"}
                """.formatted(UUID.randomUUID(), orderId));

        await().atMost(WAIT).untilAsserted(() -> assertThat(reservationStatus(orderId)).isEqualTo("RELEASED"));
        assertThat(stockOf(bookId)).as("down once, back once").isEqualTo(10);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from catalog_outbox where aggregate_id = ?", Integer.class, orderId))
                .as("one reply, not two").isOne();
    }

    @Test
    @DisplayName("OrderCancelled gives a confirmed order's units back")
    void orderCancelled() throws Exception {
        UUID bookId = newBook(10);
        UUID orderId = UUID.randomUUID();
        kafkaTemplate.send(STOCK_COMMANDS, orderId.toString(), reserve(UUID.randomUUID(), orderId, bookId, 4));
        awaitReply(orderId);

        kafkaTemplate.send("bookland.orders.order-events", orderId.toString(), """
                {"messageId":"%s","type":"OrderCancelled","orderId":"%s"}
                """.formatted(UUID.randomUUID(), orderId));

        await().atMost(WAIT).untilAsserted(() -> assertThat(stockOf(bookId)).isEqualTo(10));
        assertThat(reservationStatus(orderId)).isEqualTo("RELEASED");
    }

    /** The producer (reviews, in the monolith) is not here: the event is the JSON it publishes. */
    @Test
    @DisplayName("BookRatingChanged sets the book's average rating")
    void ratingChanged() throws Exception {
        UUID bookId = newBook(1);

        kafkaTemplate.send("bookland.reviews.book-rating-changed", bookId.toString(), """
                {"eventId":"%s","bookId":"%s","averageRating":4.5}
                """.formatted(UUID.randomUUID(), bookId));

        await().atMost(WAIT).untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                "select avg_rating from books where id = ?", Double.class, bookId)).isEqualTo(4.5));
    }

    // --- helpers ---

    private UUID newBook(int stock) throws Exception {
        String isbn = "978" + String.format("%010d", (long) (Math.random() * 1e10));
        String body = mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + tokens.forRole("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Messaging %s", "isbn": "%s", "authors": ["Tester"],
                                 "price": 40.00, "stockQuantity": %d,
                                 "categoryId": "c3d4e5f6-a7b8-9012-cdef-123456789012"}
                                """.formatted(isbn, isbn, stock)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(body).get("id").asText());
    }

    private static String reserve(UUID messageId, UUID orderId, UUID bookId, int quantity) {
        return """
                {"messageId":"%s","type":"ReserveStock","orderId":"%s","items":[{"bookId":"%s","quantity":%d}]}
                """.formatted(messageId, orderId, bookId, quantity);
    }

    private int stockOf(UUID bookId) {
        return jdbcTemplate.queryForObject("select stock_quantity from books where id = ?", Integer.class, bookId);
    }

    private String reservationStatus(UUID orderId) {
        List<String> found = jdbcTemplate.queryForList(
                "select status from stock_reservations where order_id = ?", String.class, orderId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The catalog's reply for an order, read from the broker the way orders would read it. */
    private JsonNode awaitReply(UUID orderId) {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString(),
                ConsumerConfig.GROUP_ID_CONFIG, "reply-reader-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (Consumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(STOCK_REPLIES));
            Instant deadline = Instant.now().plus(WAIT);
            while (Instant.now().isBefore(deadline)) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    if (orderId.toString().equals(record.key())) {
                        return json.readTree(record.value());
                    }
                }
            }
        }
        return fail("No stock reply for order " + orderId);
    }
}
