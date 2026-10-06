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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The author's name comes from the caller's access token and is written onto the review, so neither
 * creating nor listing a review asks the user module anything — the precondition for running the
 * user module as a service of its own.
 *
 * <p>This process has <em>no {@code users} table</em> since the identity service was extracted — the
 * test asserts it — so the name can only have come from the token.
 *
 * <p>Runs end to end because the name crosses the resolver, the command, the JPA mapping and the
 * listing — a link any unit test would mock away.
 */
@BooklandIntegrationTest
class ReviewAuthorNameIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    @DisplayName("a review stores the name its author's token carries, with no user lookup")
    void reviewStoresTheNameFromTheToken() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID bookId = jdbcTemplate.queryForObject("select id from books where active = true limit 1", UUID.class);
        deliverOrderOf(customerId, bookId);

        mockMvc.perform(post("/api/v1/books/" + bookId + "/reviews")
                        .header("Authorization", "Bearer " + tokens.forCaller(customerId, "CUSTOMER", "Ana do Token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating": 5, "comment": "Great"}
                                """))
                .andExpect(status().isCreated());

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'public' and table_name = 'users'", Integer.class))
                .as("this process keeps no users at all: the author exists only in the token").isZero();
        assertThat(jdbcTemplate.queryForObject(
                "select customer_name from reviews where customer_id = ? and book_id = ?",
                String.class, customerId, bookId))
                .isEqualTo("Ana do Token");
        assertThat(listedNameOf(customerId, bookId)).isEqualTo("Ana do Token");
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

    private String listedNameOf(UUID customerId, UUID bookId) throws Exception {
        String body = mockMvc.perform(get("/api/v1/books/" + bookId + "/reviews?size=100"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode review : json.readTree(body).get("reviews").get("content")) {
            if (review.get("customerId").asText().equals(customerId.toString())) {
                return review.get("customerName").asText();
            }
        }
        throw new AssertionError("the review is not in the listing");
    }
}
