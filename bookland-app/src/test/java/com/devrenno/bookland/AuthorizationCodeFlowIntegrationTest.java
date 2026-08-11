package com.devrenno.bookland;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives the authorization code flow end to end, because several of the ways this can break are
 * invisible to every other kind of test.
 *
 * <p>Three of them in particular:
 *
 * <ul>
 *   <li><strong>The code exchange loads the stored authorization back from the database</strong>,
 *       which deserialises the principal. A custom {@code UserDetails} missing from the polymorphic
 *       typing allowlist fails exactly there — not at startup, not at login, but on the first
 *       exchange, as a 500 from a server that was working a second earlier.</li>
 *   <li><strong>Registering must leave the caller signed in.</strong> That is a session written by
 *       hand, and nothing but a second request carrying the cookie can prove it worked.</li>
 *   <li><strong>The subject has to survive the whole round trip.</strong> The customizer unit test
 *       proves the claim is written; only this proves it is still the user id after being stored,
 *       reloaded and signed.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuthorizationCodeFlowIntegrationTest {

    private static final String REDIRECT_URI = "http://127.0.0.1:8080/authorized";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${bookland.oauth2.client.client-id}")
    private String clientId;

    @Value("${bookland.oauth2.client.client-secret}")
    private String clientSecret;

    @Value("${bookland.oauth2.api-audience}")
    private String apiAudience;

    private final ObjectMapper json = new ObjectMapper();

    @Test
    @DisplayName("registering, authorizing and exchanging the code yields tokens whose sub is the user id")
    void fullFlowCarriesTheUserIdIntoBothTokens() throws Exception {
        Registration registration = register();
        String verifier = "a".repeat(64);

        String code = authorize(registration.session(), challengeFor(verifier));
        JsonNode tokens = exchange(code, verifier);

        assertThat(tokens.has("access_token")).isTrue();
        assertThat(tokens.has("id_token")).isTrue();

        assertThat(subjectOf(tokens.get("access_token").asText()))
                .isEqualTo(registration.userId().toString());
        assertThat(subjectOf(tokens.get("id_token").asText()))
                .isEqualTo(registration.userId().toString());
    }

    /**
     * The handoff registering now performs instead of issuing tokens. If the session were not
     * established, {@code /oauth2/authorize} would redirect to the login form and
     * {@link #authorize} would find no code — so the assertion is the flow completing at all.
     */
    @Test
    @DisplayName("after registering, authorizing does not ask for the password a second time")
    void registrationSignsTheCallerIn() throws Exception {
        Registration registration = register();

        MvcResult result = mockMvc.perform(
                        get(authorizeUri(challengeFor("a".repeat(64)))).session(registration.session()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String location = result.getResponse().getHeader("Location");
        assertThat(location).startsWith(REDIRECT_URI).contains("code=");
        assertThat(location).doesNotContain("/login");
    }

    /**
     * The access token names the API; the id_token stays addressed to the client. Without the
     * difference an audience validator has nothing to separate, since the generator gives both the
     * client id.
     */
    @Test
    @DisplayName("the two tokens are addressed to different audiences")
    void audiencesDiffer() throws Exception {
        Registration registration = register();
        String verifier = "b".repeat(64);
        JsonNode tokens = exchange(authorize(registration.session(), challengeFor(verifier)), verifier);

        assertThat(audienceOf(tokens.get("access_token").asText())).contains(apiAudience);
        assertThat(audienceOf(tokens.get("id_token").asText())).contains(clientId);
    }

    /**
     * The consequence of the line above: an id_token is signed by the same key, carries the same
     * issuer and is perfectly valid — and must still not open the API.
     */
    @Test
    @DisplayName("an id_token from the real flow is refused as a Bearer credential")
    void idTokenIsNotAnApiCredential() throws Exception {
        Registration registration = register();
        String verifier = "c".repeat(64);
        JsonNode tokens = exchange(authorize(registration.session(), challengeFor(verifier)), verifier);

        mockMvc.perform(get("/api/v1/cart")
                        .header("Authorization", "Bearer " + tokens.get("id_token").asText()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/cart")
                        .header("Authorization", "Bearer " + tokens.get("access_token").asText()))
                .andExpect(status().isOk());
    }

    /**
     * The stored authorization carries the whole {@code Authentication}, principal included. Spring
     * erases credentials only from a principal implementing {@code CredentialsContainer}; ours did
     * not at first, and the BCrypt hash was being written into this column — a credential copied
     * into a second table, with a different lifetime, reached by code that has no business handling
     * one.
     */
    @Test
    @DisplayName("no password hash is written into the stored authorization")
    void storedAuthorizationCarriesNoPasswordHash() throws Exception {
        Registration registration = register();
        String verifier = "d".repeat(64);
        exchange(authorize(registration.session(), challengeFor(verifier)), verifier);

        String attributes = jdbcTemplate.queryForObject(
                "select attributes from oauth2_authorization order by access_token_issued_at desc limit 1",
                String.class);

        assertThat(attributes).contains("BooklandUserDetails");
        assertThat(attributes).contains("\"passwordHash\":null");
        assertThat(attributes).doesNotContain("$2a$");
    }

    // --- flow steps ----------------------------------------------------------------------------

    private Registration register() throws Exception {
        String email = "flow-" + UUID.randomUUID() + "@bookland.com";

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Flow Tester", "email": "%s", "password": "senha1234"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.has("accessToken")).isFalse();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).as("registering must establish a session").isNotNull();

        return new Registration(UUID.fromString(body.get("id").asText()), session);
    }

    private String authorize(MockHttpSession session, String challenge) throws Exception {
        MvcResult result = mockMvc.perform(get(authorizeUri(challenge)).session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String location = result.getResponse().getHeader("Location");
        assertThat(location).as("authorize must return a code, not the login form").contains("code=");
        return location.replaceAll(".*code=([^&]+).*", "$1");
    }

    private JsonNode exchange(String code, String verifier) throws Exception {
        MvcResult result = mockMvc.perform(post("/oauth2/token")
                        .with(httpBasic(clientId, clientSecret))
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("redirect_uri", REDIRECT_URI)
                        .param("code_verifier", verifier))
                .andExpect(status().isOk())
                .andReturn();

        return json.readTree(result.getResponse().getContentAsString());
    }

    // --- helpers -------------------------------------------------------------------------------

    /**
     * Two MockMvc details, both of which cost a debugging round:
     *
     * <p>The parameters go in the query string rather than through {@code .param(...)} — the
     * endpoint reads the authorization request off the query string, and a request built with
     * {@code .param} has a populated parameter map but a null query string, which the endpoint
     * reports as a missing {@code response_type}.
     *
     * <p>And the values go in raw: MockMvc encodes the URI it is given, so pre-encoding the
     * redirect URI produces a double-encoded value that matches nothing registered.
     */
    private String authorizeUri(String challenge) {
        return "/oauth2/authorize"
                + "?response_type=code"
                + "&client_id=" + clientId
                + "&redirect_uri=" + REDIRECT_URI
                + "&scope=openid profile email"
                + "&code_challenge=" + challenge
                + "&code_challenge_method=S256";
    }

    private String challengeFor(String verifier) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(verifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    private String subjectOf(String jwt) {
        return payloadOf(jwt).get("sub").asText();
    }

    private String audienceOf(String jwt) {
        return payloadOf(jwt).get("aud").toString();
    }

    private JsonNode payloadOf(String jwt) {
        String payload = new String(
                Base64.getUrlDecoder().decode(jwt.split("\\.")[1]), StandardCharsets.UTF_8);
        return json.readTree(payload);
    }

    private record Registration(UUID userId, MockHttpSession session) {
    }
}
