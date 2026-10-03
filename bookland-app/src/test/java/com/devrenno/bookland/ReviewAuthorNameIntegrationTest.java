package com.devrenno.bookland;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The author's name is written onto the review when it is created and read back from there, so
 * listing reviews no longer asks the user module for each author. That is a precondition for
 * extracting the user module into its own service: across a network, the old lookup would be one
 * call per author on every listing.
 *
 * <p>Runs end to end because the change spans a migration, the JPA mapping and two use cases — a
 * column the mapping forgot would pass every unit test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ReviewAuthorNameIntegrationTest {

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
    @DisplayName("a review keeps the name its author had when writing it")
    void reviewKeepsTheNameItWasWrittenWith() throws Exception {
        UUID customerId = register("Ana Original");
        UUID bookId = jdbcTemplate.queryForObject("select id from books where active = true limit 1", UUID.class);
        deliverOrderOf(customerId, bookId);

        mockMvc.perform(post("/api/v1/books/" + bookId + "/reviews")
                        .header("Authorization", "Bearer " + tokens.forCaller(customerId, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating": 5, "comment": "Great"}
                                """))
                .andExpect(status().isCreated());

        assertThat(jdbcTemplate.queryForObject(
                "select customer_name from reviews where customer_id = ? and book_id = ?",
                String.class, customerId, bookId))
                .isEqualTo("Ana Original");

        jdbcTemplate.update("update users set name = 'Ana Renamed' where id = ?", customerId);

        assertThat(listedNameOf(customerId, bookId)).isEqualTo("Ana Original");
    }

    private UUID register(String name) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "email": "review-%s@bookland.com", "password": "senha1234"}
                                """.formatted(name, UUID.randomUUID())))
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
