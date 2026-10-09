package com.devrenno.bookland;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The login form the authorization code flow lands on: ours, dressed as the storefront, while the
 * processing stays Spring Security's. {@code AuthorizationCodeFlowIntegrationTest} covers posting
 * to it; this pins what the page itself must carry for that post to work.
 */
@IdentityIntegrationTest
class LoginPageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("serves the storefront's login page, not the generated one")
    void servesTheStyledPage() throws Exception {
        mockMvc.perform(get("/login").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<title>Entrar · Bookland</title>")))
                // The generated page's heading — gone once a login page is named.
                .andExpect(content().string(not(containsString("Please sign in"))));
    }

    @Test
    @DisplayName("posts what the framework's filter reads: username, password and the CSRF token")
    void carriesTheFieldsTheFilterExpects() throws Exception {
        mockMvc.perform(get("/login").accept(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("action=\"/login\"")))
                .andExpect(content().string(containsString("name=\"username\"")))
                .andExpect(content().string(containsString("name=\"password\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    @DisplayName("links back to the storefront, where accounts are created")
    void linksToTheStorefront() throws Exception {
        mockMvc.perform(get("/login").accept(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("href=\"http://127.0.0.1:3000/register\"")))
                .andExpect(content().string(containsString("href=\"http://127.0.0.1:3000/\"")));
    }

    @Test
    @DisplayName("says the credentials were wrong without saying which")
    void reportsAFailedLogin() throws Exception {
        mockMvc.perform(get("/login").param("error", "").accept(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("E-mail ou senha incorretos.")));
    }

    @Test
    @DisplayName("shows no error on a first visit")
    void quietOnAFirstVisit() throws Exception {
        mockMvc.perform(get("/login").accept(MediaType.TEXT_HTML))
                .andExpect(content().string(not(containsString("E-mail ou senha incorretos."))));
    }

    @Test
    @DisplayName("stays out of the API document")
    void isNotPartOfTheApi() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/login']").doesNotExist());
    }
}
