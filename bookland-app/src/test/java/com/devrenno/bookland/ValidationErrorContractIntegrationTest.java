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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Locks the 400 half of the error contract (docs/error-contract.md) on this process's routes. The
 * shape of the {@code errors} map and the English messages are pinned in full by the identity
 * service's twin, on the public register endpoint; the handler is the same
 * {@code ValidationExceptionHandler} from web-support in both.
 */
@BooklandIntegrationTest
class ValidationErrorContractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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

    /**
     * The catalog carries the most varchar(255) columns, and coverImageUrl was the worst of them:
     * it declared @Size(max = 2048) against a varchar(255) column, so every URL between the two
     * bounds was accepted by validation purely to fail at the database.
     */
    @Test
    @DisplayName("catalog fields are bounded by their columns, so a long value is a 400 not a 500")
    void bookFieldsAreBoundedByTheirColumns() throws Exception {
        String adminToken = tokens.forRole("ADMIN");

        mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "%s", "isbn": "9781234567897", "authors": ["A"],
                                 "price": 10.00, "stockQuantity": 1,
                                 "categoryId": "00000000-0000-0000-0000-000000000000",
                                 "coverImageUrl": "https://example.com/%s.jpg"}
                                """.formatted("T".repeat(300), "u".repeat(300))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.title").isArray())
                .andExpect(jsonPath("$.errors.coverImageUrl").isArray());
    }

    @Test
    @DisplayName("unparseable body: 400 MALFORMED_REQUEST, no parser internals leaked")
    void malformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + tokens.forRole("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.detail").value("The request body is missing or is not valid JSON"));
    }

    @Test
    @DisplayName("unconvertible path variable: 400 INVALID_PARAMETER, keyed by parameter name")
    void malformedPathVariable() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/books/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }
}
