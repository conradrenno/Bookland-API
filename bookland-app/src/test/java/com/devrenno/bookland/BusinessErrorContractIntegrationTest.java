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

    /**
     * The container forwards an unhandled exception to /error. While that path required
     * authentication, the forward was answered with 401 TOKEN_MISSING and the real failure never
     * reached the client — worse than losing it, because a client treats 401 as "refresh and
     * retry" and ends the session over a server bug.
     */
    /**
     * The half of the masking bug that lives in the security rules. While /error required
     * authentication, the container's forward after an unhandled exception was answered with
     * 401 TOKEN_MISSING and the real failure never reached the client — and a client reads 401 as
     * "refresh and retry", so a server bug ended the session instead of surfacing.
     *
     * <p>What the error dispatch then renders is covered by ProblemDetailErrorControllerTest:
     * MockMvc does not run the ERROR dispatch, so it cannot be asserted end to end here.
     */
    @Test
    @DisplayName("/error is reachable without a token, or every 500 arrives as a fake 401")
    void errorPathIsNotBehindAuthentication() throws Exception {
        mockMvc.perform(get("/error"))
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not("TOKEN_MISSING")))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    /**
     * The flip side, and correct: an unauthenticated request to a path behind the authentication
     * wall is 401 whether or not that path exists. Answering 404 there would let anyone map the
     * API's private routes.
     */
    @Test
    @DisplayName("an unmapped protected path stays 401, and does not reveal that it is unmapped")
    void unmappedProtectedPathDoesNotLeakItsAbsence() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_MISSING"));
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
