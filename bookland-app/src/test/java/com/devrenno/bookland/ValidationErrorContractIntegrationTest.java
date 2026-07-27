package com.devrenno.bookland;

import com.devrenno.bookland.auth.application.port.out.TokenProviderPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Locks the 400 half of the error contract (docs/error-contract.md): one {@code errors} map keyed
 * by field, and English messages regardless of the locale the JVM happens to run in.
 *
 * <p>{@code POST /api/v1/auth/register} is the vehicle because it is public — no token needed — and
 * its password carries three constraints at once, which is what proves a field keeps all of its
 * messages instead of only the first.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ValidationErrorContractIntegrationTest {

    private static final String REGISTER = "/api/v1/auth/register";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenProviderPort tokenProvider;

    @Test
    @DisplayName("every broken field lands in errors, keyed by its own name")
    void errorsAreKeyedByField() throws Exception {
        mockMvc.perform(post(REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "  ", "email": "not-an-email", "password": "short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value(REGISTER))
                .andExpect(jsonPath("$.errors.name").isArray())
                .andExpect(jsonPath("$.errors.email").isArray())
                .andExpect(jsonPath("$.errors.password").isArray());
    }

    @Test
    @DisplayName("a field breaking several constraints keeps every message")
    void oneFieldCanCarrySeveralMessages() throws Exception {
        mockMvc.perform(post(REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Renno", "email": "renno@bookland.com", "password": "short"}
                                """))
                .andExpect(status().isBadRequest())
                // too short AND missing a digit
                .andExpect(jsonPath("$.errors.password", hasSize(2)));
    }

    @Test
    @DisplayName("messages are English whatever the JVM default locale is")
    void messagesAreEnglish() throws Exception {
        mockMvc.perform(post(REGISTER)
                        .header("Accept-Language", "pt-BR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "  ", "email": "not-an-email", "password": "12345678"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name[0]").value("must not be blank"))
                .andExpect(jsonPath("$.errors.email[0]").value("must be a well-formed email address"));
    }

    @Test
    @DisplayName("no message names its own field — the map key already does")
    void messagesAreNotFieldPrefixed() throws Exception {
        mockMvc.perform(post(REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Renno", "email": "renno@bookland.com", "password": "nodigits"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password", everyItem(matchesPattern("^(must|size|is) .*"))));
    }

    @Test
    @DisplayName("detail stays populated, for use as a form-level banner")
    void detailSummarisesTheFailure() throws Exception {
        mockMvc.perform(post(REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "  ", "email": "not-an-email", "password": "short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    /**
     * The column is varchar(255) and the field had no upper bound, so an over-long name reached the
     * database and came back as a 500 — which, while /error was authenticated, reached the client
     * as 401 TOKEN_MISSING. It is a rejected field, and has to be answered as one.
     */
    @Test
    @DisplayName("a value longer than its column is a 400 naming the field, not a 500")
    void overlongValueIsRejectedBeforeTheDatabase() throws Exception {
        mockMvc.perform(post(REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "email": "long@bookland.com", "password": "senha1234"}
                                """.formatted("N".repeat(300))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.name").isArray());
    }

    /**
     * The catalog carries the most varchar(255) columns, and coverImageUrl was the worst of them:
     * it declared @Size(max = 2048) against a varchar(255) column, so every URL between the two
     * bounds was accepted by validation purely to fail at the database.
     */
    @Test
    @DisplayName("catalog fields are bounded by their columns, so a long value is a 400 not a 500")
    void bookFieldsAreBoundedByTheirColumns() throws Exception {
        String adminToken = tokenProvider
                .generate(UUID.randomUUID().toString(), "admin@bookland.com", "ADMIN")
                .value();

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
        mockMvc.perform(post(REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": "))
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
