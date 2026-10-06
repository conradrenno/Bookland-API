package com.devrenno.bookland;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The API's Swagger UI, served from http://127.0.0.1:8080, exchanges the authorization code with a
 * {@code fetch} to this server's token endpoint. Across origins the browser first sends a preflight
 * {@code OPTIONS} and only proceeds if the answer names the page's origin; without it the login
 * fails after the password, as "Failed to fetch", with nothing in the server log.
 */
@IdentityIntegrationTest
class TokenEndpointCorsIntegrationTest {

    private static final String API_ORIGIN = "http://127.0.0.1:8080";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("the token endpoint admits a preflight from the API's origin")
    void preflightFromTheApiIsAllowed() throws Exception {
        mockMvc.perform(options("/oauth2/token")
                        .header("Origin", API_ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", API_ORIGIN));
    }

    @Test
    @DisplayName("any other origin is refused")
    void preflightFromAnotherOriginIsRefused() throws Exception {
        mockMvc.perform(options("/oauth2/token")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
