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

    @Test
    @DisplayName("unparseable body: 400 MALFORMED_REQUEST, no parser internals leaked")
    void malformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + tokens.forRole("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.detail").value("The request body is missing or is not valid JSON"));
    }

    @Test
    @DisplayName("unconvertible path variable: 400 INVALID_PARAMETER, keyed by parameter name")
    void malformedPathVariable() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/books/not-a-uuid/reviews"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    /**
     * Paging parameters are validated at the door. Until step 5b a negative page reached PageQuery,
     * threw IllegalArgumentException, and the catalog's handler — global in the monolith — answered
     * 400; with the catalog gone the same request was a 500. Now the constraint on the parameter
     * answers, with the field named.
     */
    @org.junit.jupiter.params.ParameterizedTest(name = "{0}")
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "/api/v1/books/00000000-0000-0000-0000-000000000000/reviews?page=-1",
            "/api/v1/books/00000000-0000-0000-0000-000000000000/reviews?size=0",
            "/api/v1/orders?page=-1"})
    @DisplayName("an out-of-range paging parameter: 400 VALIDATION_ERROR, not a 500")
    void pagingParametersAreValidated(String uri) throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(uri)
                        .header("Authorization", "Bearer " + tokens.forRole("CUSTOMER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }
}
