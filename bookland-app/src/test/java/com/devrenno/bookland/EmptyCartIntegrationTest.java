package com.devrenno.bookland;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A customer who never added anything has no cart, and looking at it must not pretend otherwise.
 *
 * <p>The answer used to be an unsaved cart built on the spot, so every call reported a different id
 * and a fresh updatedAt for a cart that existed nowhere — a client caching the id, or showing "last
 * changed", was being lied to on each request.
 */
@BooklandIntegrationTest
class EmptyCartIntegrationTest {

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

    private TestAccessTokens tokens;

    @BeforeEach
    void setUp() {
        tokens = new TestAccessTokens(jwkSource, issuer, apiAudience);
    }

    @Test
    @DisplayName("an empty cart has no id, no timestamp, and reading it twice creates nothing")
    void readingAnAbsentCartCreatesNothing() throws Exception {
        UUID customerId = UUID.randomUUID();
        String token = tokens.forCaller(customerId, "CUSTOMER");

        for (int call = 0; call < 2; call++) {
            mockMvc.perform(get("/api/v1/cart").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").doesNotExist())
                    .andExpect(jsonPath("$.updatedAt").doesNotExist())
                    .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                    .andExpect(jsonPath("$.items").isEmpty())
                    .andExpect(jsonPath("$.total").value(0));
        }

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from carts where customer_id = ?", Integer.class, customerId)).isZero();
    }
}
