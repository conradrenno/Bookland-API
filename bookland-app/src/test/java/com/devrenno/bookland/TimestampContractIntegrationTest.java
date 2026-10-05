package com.devrenno.bookland;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
    private JWKSource<SecurityContext> jwkSource;

    @Value("${bookland.oauth2.issuer}")
    private String issuer;

    @Value("${bookland.oauth2.api-audience}")
    private String apiAudience;

    private TestAccessTokens tokens;

    @BeforeEach
    void setUp() {
        tokens = new TestAccessTokens(jwkSource, issuer, apiAudience);
    }

    @Test
    @DisplayName("the cart reports updatedAt as a zoned instant")
    void cartDateCarriesZone() throws Exception {
        mockMvc.perform(get("/api/v1/cart").header("Authorization", "Bearer " + tokens.forRole("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedAt").value(matchesPattern(INSTANT_WITH_ZONE)));
    }

}
