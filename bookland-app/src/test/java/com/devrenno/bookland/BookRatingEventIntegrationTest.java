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
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A book's rating travels from the reviews module to the catalog as a Kafka event, not as a call: the
 * request that publishes or moderates a review stores the event in the outbox and returns, and the
 * relay sends it — so the test waits for the event on the topic rather than reading it right after
 * the response.
 *
 * <p>Since step 5b the catalog is another process, so this is the producer's half, against a real
 * broker: the topic, the key (the book id) and the resulting average in the JSON. The consumer's half
 * — that average written to the book — is the catalog service's CatalogMessagingIntegrationTest.
 */
@BooklandIntegrationTest
class BookRatingEventIntegrationTest {

    private static final Duration DELIVERY = Duration.ofSeconds(30);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FakeCatalog catalog;

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private JWKSource<SecurityContext> jwkSource;

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
    @DisplayName("publishing and moderating a review publish the book's resulting rating to Kafka")
    void ratingFollowsTheReviews() throws Exception {
        UUID bookId = catalog.addBook("30.00", 5);
        // The reviewer exists only in the token: this process keeps no users.
        UUID customerId = UUID.randomUUID();
        deliverOrderOf(customerId, bookId);

        String created = mockMvc.perform(post("/api/v1/books/" + bookId + "/reviews")
                        .header("Authorization", "Bearer " + tokens.forCaller(customerId, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating": 4, "comment": "Good"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        await().atMost(DELIVERY).untilAsserted(() -> assertThat(latestRatingPublishedFor(bookId)).isEqualTo(4.0));

        UUID reviewId = UUID.fromString(json.readTree(created).get("id").asText());
        mockMvc.perform(delete("/api/v1/books/" + bookId + "/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + tokens.forRole("ADMIN")))
                .andExpect(status().isNoContent());

        await().atMost(DELIVERY).untilAsserted(() -> assertThat(latestRatingPublishedFor(bookId)).isZero());

        // Both events went through the outbox: one row per change, each stamped by the relay.
        await().atMost(DELIVERY).untilAsserted(() -> assertThat(jdbcTemplate.queryForList(
                "select published_at from reviews_outbox where aggregate_id = ?", bookId))
                .hasSize(2)
                .allSatisfy(row -> assertThat(row.get("published_at")).isNotNull()));
    }

    /** The averageRating of the last event published for the book, read from the topic; null if none yet. */
    private Double latestRatingPublishedFor(UUID bookId) {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString(),
                ConsumerConfig.GROUP_ID_CONFIG, "rating-reader-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        Double latest = null;
        try (Consumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of("bookland.reviews.book-rating-changed"));
            for (int poll = 0; poll < 4; poll++) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    if (bookId.toString().equals(record.key())) {
                        latest = json.readTree(record.value()).get("averageRating").asDouble();
                    }
                }
            }
        }
        return latest;
    }

    /** The purchase check wants a DELIVERED order with the book; driving the order lifecycle to get one is not what this test is about. */
    private void deliverOrderOf(UUID customerId, UUID bookId) {
        UUID orderId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into orders (id, customer_id, status, total_amount, created_at, updated_at)
                values (?, ?, 'DELIVERED', 10.00, current_timestamp, current_timestamp)
                """, orderId, customerId);
        jdbcTemplate.update("""
                insert into order_items (id, order_id, book_id, title, quantity, unit_price)
                values (?, ?, ?, 'A book', 1, 10.00)
                """, UUID.randomUUID(), orderId, bookId);
    }
}
