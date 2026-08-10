package com.devrenno.bookland.auth.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Guarantees the one registered client exists, the same way {@code AdminBootstrap} guarantees the
 * admin user: look before writing, so a restart is a no-op. The dev database survives a devtools
 * restart and Flyway no longer wipes it, so a bootstrap that inserted blindly would fail on the
 * second start.
 *
 * <p>Flyway owns the table, this owns the row. The split exists because the values are per
 * environment — a redirect URI and a secret are deployment data, not schema, and putting them in
 * SQL would mean a migration per environment.
 *
 * <p>It lives in the auth module rather than in {@code bookland-app} — unlike {@code AdminBootstrap},
 * which crosses modules — so that it travels with the Authorization Server when that is extracted.
 */
@Component
@RequiredArgsConstructor
public class ClientBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ClientBootstrap.class);

    private final RegisteredClientRepository registeredClientRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthorizationServerProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        AuthorizationServerProperties.Client config = properties.getClient();

        if (registeredClientRepository.findByClientId(config.getClientId()) != null) {
            log.info("[BOOTSTRAP] OAuth2 client already registered — skipping ({})", config.getClientId());
            return;
        }

        RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(config.getClientId())
                .clientSecret(passwordEncoder.encode(config.getClientSecret()))
                .clientName(config.getClientName())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUris(uris -> uris.addAll(config.getRedirectUris()))
                .postLogoutRedirectUris(uris -> uris.addAll(config.getPostLogoutRedirectUris()))
                .scopes(scopes -> scopes.addAll(config.getScopes()))
                .clientSettings(ClientSettings.builder()
                        // PKCE required. It binds the code to whoever started the flow, which is
                        // what makes an intercepted code useless on its own.
                        .requireProofKey(true)
                        // First-party client: asking the user to approve Bookland to Bookland is
                        // noise. The consent table stays empty.
                        .requireAuthorizationConsent(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(config.getAccessTokenTtlMinutes()))
                        .refreshTokenTimeToLive(Duration.ofDays(config.getRefreshTokenTtlDays()))
                        // Single use: refreshing rotates the token. A replayed refresh token is
                        // then a detectable event rather than a working credential.
                        .reuseRefreshTokens(false)
                        .build())
                .build();

        registeredClientRepository.save(client);
        log.info("[BOOTSTRAP] OAuth2 client registered — {}", config.getClientId());
    }
}
