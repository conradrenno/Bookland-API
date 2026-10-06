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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Locks the 401/403 half of the error contract (docs/error-contract.md).
 *
 * <p>Runs against the real security filter chain on purpose: the behaviour under test — which
 * denial produces which status and code — is decided entirely inside it, so a unit test against
 * mocked ports would prove nothing.
 */
@BooklandIntegrationTest
class AuthErrorContractIntegrationTest {

    private static final String CUSTOMER_ROUTE = "/api/v1/cart";
    private static final String ADMIN_ROUTE = "/api/v1/admin/orders";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JWKSource<SecurityContext> jwkSource;

    @Value("${bookland.resource-server.issuer}")
    private String issuer;

    @Value("${bookland.resource-server.audience}")
    private String apiAudience;

    /** What an id_token carries as its audience: the registered client's id, not the API's. */
    private static final String CLIENT_ID = "bookland-web";

    private TestAccessTokens tokens;

    @BeforeEach
    void setUp() {
        tokens = new TestAccessTokens(jwkSource, issuer, apiAudience);
    }

    @Test
    @DisplayName("no token: 401 TOKEN_MISSING with a bare Bearer challenge")
    void missingToken() throws Exception {
        mockMvc.perform(get(CUSTOMER_ROUTE))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.code").value("TOKEN_MISSING"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.instance").value(CUSTOMER_ROUTE));
    }

    @Test
    @DisplayName("malformed token: 401 TOKEN_INVALID")
    void malformedToken() throws Exception {
        mockMvc.perform(get(CUSTOMER_ROUTE).header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate",
                        org.hamcrest.Matchers.containsString("error=\"invalid_token\"")))
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
    }

    @Test
    @DisplayName("expired token: 401 TOKEN_EXPIRED — the signal for the client to refresh")
    void expiredToken() throws Exception {
        mockMvc.perform(get(CUSTOMER_ROUTE).header("Authorization", "Bearer " + tokens.expired()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
    }

    @Test
    @DisplayName("no token on an admin route: 401, not 403 — the client has to log in first")
    void missingTokenOnAdminRoute() throws Exception {
        mockMvc.perform(get(ADMIN_ROUTE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_MISSING"));
    }

    @Test
    @DisplayName("valid CUSTOMER token on an admin route: 403 INSUFFICIENT_ROLE — refreshing is pointless")
    void validTokenWithoutTheRole() throws Exception {
        mockMvc.perform(get(ADMIN_ROUTE).header("Authorization", "Bearer " + tokens.forRole("CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_ROLE"))
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    /**
     * Pins the rule to the whole {@code /api/v1/admin} prefix rather than to the controllers that
     * exist today. Scoped to {@code /admin/orders/**}, this route fell through to
     * {@code anyRequest().authenticated()} and a CUSTOMER got a 404 — the same path a new admin
     * controller would take, answering 200.
     */
    @Test
    @DisplayName("CUSTOMER token on an admin route no controller serves yet: still 403")
    void adminPrefixIsClosedByDefault() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + tokens.forRole("CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_ROLE"));
    }

    /**
     * The other half of the rule above, and the one that fails loudly if the {@code role} claim ever
     * stops being mapped to an authority: without {@code JwtAuthenticationConverter} the token still
     * verifies, the caller is still authenticated, and every admin route answers 403 — including to
     * an admin.
     */
    @Test
    @DisplayName("the role claim becomes an authority, so an ADMIN token reaches an admin route")
    void adminRoleClaimGrantsTheAdminRoute() throws Exception {
        mockMvc.perform(get(ADMIN_ROUTE).header("Authorization", "Bearer " + tokens.forRole("ADMIN")))
                .andExpect(status().isOk());
    }

    /**
     * The audience half of the contract. An {@code id_token} is addressed to the client, carries the
     * same issuer and the same signature, and would otherwise be a perfectly good Bearer credential:
     * the default decoder validates no audience at all, and the generator gives both tokens
     * {@code aud = client_id} unless the customizer intervenes.
     */
    @Test
    @DisplayName("a token addressed to the client, not the API, is refused as invalid")
    void tokenForAnotherAudienceIsRefused() throws Exception {
        mockMvc.perform(get(CUSTOMER_ROUTE)
                        .header("Authorization", "Bearer " + tokens.addressedTo(CLIENT_ID)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
    }

    @Test
    @DisplayName("every denial is problem+json, never an empty body")
    void deniedRequestsCarryAProblemDetailBody() throws Exception {
        mockMvc.perform(get(CUSTOMER_ROUTE))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type",
                        org.hamcrest.Matchers.containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }
}
