package com.devrenno.bookland;

import com.devrenno.bookland.auth.application.port.out.TokenProviderPort;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

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
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuthErrorContractIntegrationTest {

    private static final String CUSTOMER_ROUTE = "/api/v1/cart";
    private static final String ADMIN_ROUTE = "/api/v1/admin/orders";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenProviderPort tokenProvider;

    @Value("${bookland.jwt.secret}")
    private String jwtSecret;

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
        mockMvc.perform(get(CUSTOMER_ROUTE).header("Authorization", "Bearer " + expiredJwt()))
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
        String token = tokenProvider
                .generate(UUID.randomUUID().toString(), "customer@bookland.com", "CUSTOMER")
                .value();

        mockMvc.perform(get(ADMIN_ROUTE).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_ROLE"))
                .andExpect(jsonPath("$.title").value("Forbidden"));
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

    /** Signed with the real key, so it is rejected for being expired and not for being forged. */
    private String expiredJwt() {
        Instant issuedAt = Instant.now().minusSeconds(7200);
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));

        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("email", "customer@bookland.com")
                .claim("role", "CUSTOMER")
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusSeconds(60)))
                .signWith(key)
                .compact();
    }
}
