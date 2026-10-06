package com.devrenno.bookland;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The identity service's business codes, from docs/error-contract.md, against the real context.
 */
@IdentityIntegrationTest
class BusinessErrorContractIntegrationTest {

    /** Seeded by DevCustomerSeeder (bookland-user). */
    private static final String SEEDED_EMAIL = "joao@bookland.com";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("409 on a taken email carries EMAIL_ALREADY_EXISTS")
    void emailAlreadyExists() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Duplicate", "email": "%s", "password": "senha1234"}
                                """.formatted(SEEDED_EMAIL)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    /** Same reason as in the monolith: chains 3 and 4 come from web-support in both processes. */
    @Test
    @DisplayName("/error is reachable without a token, or every 500 arrives as a fake 401")
    void errorPathIsNotBehindAuthentication() throws Exception {
        mockMvc.perform(get("/error"))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }
}
