package com.devrenno.bookland;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
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
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class TimestampContractIntegrationTest {

    /** Seeded by DevDataLoader. */
    private static final String SEEDED_EMAIL = "joao@bookland.com";
    private static final String SEEDED_PASSWORD = "joao1234";

    /** ISO-8601 instant: the trailing Z is the whole point. */
    private static final String INSTANT_WITH_ZONE = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("the token pair reports expiry as a zoned instant")
    void loginDatesCarryZone() throws Exception {
        mockMvc.perform(login())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessTokenExpiresAt").value(matchesPattern(INSTANT_WITH_ZONE)))
                .andExpect(jsonPath("$.refreshTokenExpiresAt").value(matchesPattern(INSTANT_WITH_ZONE)));
    }

    @Test
    @DisplayName("the cart reports updatedAt as a zoned instant")
    void cartDateCarriesZone() throws Exception {
        mockMvc.perform(get("/api/v1/cart").header("Authorization", "Bearer " + accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedAt").value(matchesPattern(INSTANT_WITH_ZONE)));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder login() {
        return post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(SEEDED_EMAIL, SEEDED_PASSWORD));
    }

    private String accessToken() throws Exception {
        String body = mockMvc.perform(login())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll("(?s).*\"accessToken\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }
}
