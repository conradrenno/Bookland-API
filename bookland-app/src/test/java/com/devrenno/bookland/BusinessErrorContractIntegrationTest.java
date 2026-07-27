package com.devrenno.bookland;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Business rule violations carry a {@code code} too, so a client branches on a symbol instead of
 * matching on status plus route. Only the cases reachable without a fixture are covered here; the
 * codes themselves are listed in docs/error-contract.md.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class BusinessErrorContractIntegrationTest {

    /** Seeded by DevDataLoader. */
    private static final String SEEDED_EMAIL = "joao@bookland.com";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("404 from the catalog carries BOOK_NOT_FOUND")
    void bookNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/books/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOK_NOT_FOUND"))
                .andExpect(jsonPath("$.title").value("Not Found"));
    }

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

    @Test
    @DisplayName("401 on bad credentials carries INVALID_CREDENTIALS, not a token code")
    void invalidCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "wrong-password-1"}
                                """.formatted(SEEDED_EMAIL)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("a business error is not a validation error: no errors map")
    void businessErrorsCarryNoFieldMap() throws Exception {
        mockMvc.perform(get("/api/v1/books/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }
}
