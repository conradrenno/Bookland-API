package com.devrenno.bookland;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A book's rating travels from the reviews module to the catalog as a Kafka event, not as a call: the
 * request that publishes or moderates a review returns before the catalog has applied it, so the test
 * waits for the rating to arrive rather than reading it right after the response.
 *
 * <p>End to end because each half is unit-tested against the other's JSON, and only a real broker
 * shows they agree on the topic, the key and the listener's wiring.
 */
@BooklandIntegrationTest
class BookRatingEventIntegrationTest {

    private static final Duration DELIVERY = Duration.ofSeconds(30);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JWKSource<SecurityContext> jwkSource;

    @Value("${bookland.oauth2.issuer}")
    private String issuer;

    @Value("${bookland.oauth2.api-audience}")
    private String apiAudience;

    private final ObjectMapper json = new ObjectMapper();

    private TestAccessTokens tokens;

    @BeforeEach
    void setUp() {
        tokens = new TestAccessTokens(jwkSource, issuer, apiAudience);
    }

    @Test
    @DisplayName("publishing and moderating a review reach the book's rating through Kafka")
    void ratingFollowsTheReviews() throws Exception {
        UUID bookId = jdbcTemplate.queryForObject("""
                select b.id from books b
                where b.active = true
                  and not exists (select 1 from reviews r where r.book_id = b.id)
                limit 1
                """, UUID.class);
        UUID customerId = register();
        deliverOrderOf(customerId, bookId);

        String created = mockMvc.perform(post("/api/v1/books/" + bookId + "/reviews")
                        .header("Authorization", "Bearer " + tokens.forCaller(customerId, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating": 4, "comment": "Good"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        await().atMost(DELIVERY).untilAsserted(() -> assertThat(ratingOf(bookId)).isEqualTo(4.0));

        UUID reviewId = UUID.fromString(json.readTree(created).get("id").asText());
        mockMvc.perform(delete("/api/v1/books/" + bookId + "/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + tokens.forRole("ADMIN")))
                .andExpect(status().isNoContent());

        await().atMost(DELIVERY).untilAsserted(() -> assertThat(ratingOf(bookId)).isZero());
    }

    private double ratingOf(UUID bookId) {
        return jdbcTemplate.queryForObject("select avg_rating from books where id = ?", Double.class, bookId);
    }

    private UUID register() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Rating Reader", "email": "rating-%s@bookland.com", "password": "senha1234"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(body).get("id").asText());
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
