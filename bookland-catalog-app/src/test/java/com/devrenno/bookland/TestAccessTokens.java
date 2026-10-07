package com.devrenno.bookland;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Mints access tokens for the contract tests, signed with {@link TestSigningKey}.
 *
 * <p>The tokens must look exactly like the identity service's — same issuer, audience and claims
 * ({@code sub}, {@code email}, {@code role}, {@code name}) — or these tests would be checking a token
 * nobody issues. The issuer and audience come from this process's own resource-server configuration;
 * the claims are pinned on the identity side by {@code AuthorizationCodeFlowIntegrationTest}, and a
 * change there must be mirrored here.
 *
 * <p>Driving the whole browser flow to obtain one would be more end-to-end and much worse as a
 * fixture — and here impossible: the Authorization Server is another process.
 */
class TestAccessTokens {

    private static final String DEFAULT_NAME = "Test Customer";

    private final JwtEncoder encoder;
    private final String issuer;
    private final String apiAudience;

    TestAccessTokens(JWKSource<SecurityContext> jwkSource, String issuer, String apiAudience) {
        this.encoder = new NimbusJwtEncoder(jwkSource);
        this.issuer = issuer;
        this.apiAudience = apiAudience;
    }

    String forRole(String role) {
        return token(UUID.randomUUID(), role, DEFAULT_NAME, apiAudience, Instant.now(), Duration.ofMinutes(15));
    }

    String forCaller(UUID userId, String role) {
        return forCaller(userId, role, DEFAULT_NAME);
    }

    String forCaller(UUID userId, String role, String name) {
        return token(userId, role, name, apiAudience, Instant.now(), Duration.ofMinutes(15));
    }

    /** Signed with the real key, so it is refused for being expired and not for being forged. */
    String expired() {
        return token(UUID.randomUUID(), "CUSTOMER", DEFAULT_NAME, apiAudience,
                Instant.now().minus(Duration.ofHours(2)), Duration.ofMinutes(1));
    }

    /**
     * Carries the client id as its audience — which is exactly what an {@code id_token} carries, and
     * what an access token would carry without {@code BooklandTokenCustomizer}.
     */
    String addressedTo(String audience) {
        return token(UUID.randomUUID(), "CUSTOMER", DEFAULT_NAME, audience, Instant.now(), Duration.ofMinutes(15));
    }

    private String token(UUID subject, String role, String name, String audience, Instant issuedAt, Duration ttl) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(subject.toString())
                .audience(List.of(audience))
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(ttl))
                .claim("email", "customer@bookland.com")
                .claim("role", role)
                .claim("name", name)
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
