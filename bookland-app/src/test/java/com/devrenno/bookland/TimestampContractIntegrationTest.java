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

import java.util.UUID;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every date on the wire identifies an instant.
 *
 * <p>{@link com.devrenno.bookland.architecture.TimestampRulesTest} proves no {@code LocalDateTime}
 * field survives anywhere; this proves what actually reaches the client, which is the part a
 * consumer codes against. The two together are the contract: a date field ends in {@code Z}.
 */
@BooklandIntegrationTest
class TimestampContractIntegrationTest {

    /** ISO-8601 instant: the trailing Z is the whole point. */
    private static final String INSTANT_WITH_ZONE = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z";

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

    @Autowired
    private FakeCatalog catalog;

    private TestAccessTokens tokens;

    @BeforeEach
    void setUp() {
        tokens = new TestAccessTokens(jwkSource, issuer, apiAudience);
    }

    /** A cart exists only once something was added to it, so the test adds an item first. */
    @Test
    @DisplayName("the cart reports updatedAt as a zoned instant")
    void cartDateCarriesZone() throws Exception {
        String token = tokens.forRole("CUSTOMER");
        UUID bookId = catalog.addBook("30.00", 5);
        mockMvc.perform(post("/api/v1/cart/items").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": "%s", "quantity": 1}
                                """.formatted(bookId)))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(get("/api/v1/cart").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedAt").value(matchesPattern(INSTANT_WITH_ZONE)));
    }

}
