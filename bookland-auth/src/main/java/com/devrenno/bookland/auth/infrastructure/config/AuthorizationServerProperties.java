package com.devrenno.bookland.auth.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything about the Authorization Server that changes between environments: the issuer, the
 * signing key pair and the one registered client.
 *
 * <p>None of this is domain configuration — a {@code RegisteredClient} is not an entity of the
 * auth module, it is deployment data. Flyway creates the table, this class carries the values, and
 * {@link ClientBootstrap} writes the row.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "bookland.oauth2")
public class AuthorizationServerProperties {

    /**
     * Goes into the {@code iss} claim and into the discovery document, so it must be the URL
     * clients actually reach the server on. A mismatch is rejected at validation time, not at
     * issuing time — the token is emitted happily and refused everywhere.
     */
    private String issuer = "http://127.0.0.1:8080";

    /**
     * The {@code aud} of the access token. It names the resource server, not the client — which is
     * what allows the API to refuse an id_token presented as a Bearer, since by default the
     * generator gives both tokens {@code aud = client_id}.
     */
    private String apiAudience = "bookland-api";

    private final Jwk jwk = new Jwk();
    private final Client client = new Client();

    /**
     * The RSA pair used to sign tokens. Generating one per boot — as every sample does — invalidates
     * every token on restart and cannot work with more than one instance, so it is read from
     * configuration from day one.
     *
     * <p>Both halves are base64 of the DER encoding, single-line, so they fit an environment
     * variable: private in PKCS#8, public in X.509/SubjectPublicKeyInfo. Only the public half ever
     * leaves the server, through the JWKS endpoint.
     */
    @Getter
    @Setter
    public static class Jwk {
        /** Names the key in the JWKS. Rotation adds a second key and changes this. */
        private String keyId = "bookland-key-1";
        private String publicKey;
        private String privateKey;
    }

    @Getter
    @Setter
    public static class Client {
        private String clientId;
        /** Plain text here; {@link ClientBootstrap} BCrypts it before it reaches the table. */
        private String clientSecret;
        private String clientName;
        /**
         * Must be loopback IPs, not {@code localhost} — the server rejects the literal hostname,
         * following RFC 8252.
         */
        private List<String> redirectUris = new ArrayList<>();
        private List<String> postLogoutRedirectUris = new ArrayList<>();
        /** {@code openid} has to be here, or no id_token is ever issued. */
        private List<String> scopes = new ArrayList<>();
        private long accessTokenTtlMinutes = 15;
        private long refreshTokenTtlDays = 7;
    }
}
