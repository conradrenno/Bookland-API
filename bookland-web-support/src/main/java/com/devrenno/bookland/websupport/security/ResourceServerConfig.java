package com.devrenno.bookland.websupport.security;

import com.devrenno.bookland.websupport.ProblemDetailWriter;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Everything a service needs to accept a Bookland access token, in the one module every service
 * already depends on. It lived in bookland-auth, which meant a service extracted on its own would
 * have had to depend on the Authorization Server — the login form, the user module and the private
 * signing key — just to check a token.
 *
 * <p>Holds no key that can sign. The Authorization Server's {@code JWKSource} carries the private
 * half; what the decoder is built from here is the public half only, extracted from it.
 */
@Configuration
@EnableConfigurationProperties(ResourceServerProperties.class)
public class ResourceServerConfig {

    /**
     * Verifies every token the application accepts — at the API and, in the monolith, at
     * {@code /userinfo} too, since the Authorization Server's chain picks this bean up as well.
     *
     * <p>Two sources for the public keys, chosen by whether {@code jwk-set-uri} is set:
     *
     * <ul>
     *   <li><strong>Over HTTP</strong>, from the Authorization Server's {@code /oauth2/jwks}. How a
     *       service in a process of its own gets them.</li>
     *   <li><strong>In memory</strong>, from the {@code JWKSource} bean the Authorization Server
     *       publishes in the same process. Not an optimisation: the contract tests drive the
     *       application through MockMvc, which opens no port, so a decoder that fetched the JWKS
     *       over HTTP would fail every one of them.</li>
     * </ul>
     *
     * <p>Setting the validator replaces the defaults wholesale, so all four checks are listed. The
     * audience one is why this bean exists at all rather than being left to Boot: without it an
     * {@code id_token} passes as a Bearer credential, because the default decoder validates no
     * audience whatsoever.
     */
    @Bean
    public JwtDecoder jwtDecoder(ResourceServerProperties properties,
                                 ObjectProvider<JWKSource<SecurityContext>> inProcessKeys) {
        require(properties.issuer(), "issuer");
        require(properties.audience(), "audience");

        NimbusJwtDecoder decoder = StringUtils.hasText(properties.jwkSetUri())
                ? NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()).build()
                : NimbusJwtDecoder.withJwkSource(publicKeysOf(inProcessKeys.getIfAvailable(() -> {
                    throw new IllegalStateException("bookland.resource-server.jwk-set-uri is not set and no "
                            + "JWKSource bean is present: this process has no way to obtain the token "
                            + "signing keys");
                }))).build();

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtIssuerValidator(properties.issuer()),
                // Ours first, for a machine-readable expiry code; the framework's stays for nbf and
                // as the authoritative check of exp.
                new AccessTokenExpiryValidator(),
                new JwtTimestampValidator(),
                new ApiAudienceValidator(properties.audience())));

        return decoder;
    }

    /**
     * The classifier wraps the renderer: it reads the rejection reason out of the exception chain
     * and records it, then lets {@code RestAuthenticationEntryPoint} write the body. That split is
     * what preserves {@code TOKEN_EXPIRED} vs {@code TOKEN_INVALID}.
     */
    @Bean
    public AuthenticationEntryPoint restAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return new BearerTokenErrorClassifier(
                new RestAuthenticationEntryPoint(new ProblemDetailWriter(objectMapper)));
    }

    @Bean
    public AccessDeniedHandler restAccessDeniedHandler(ObjectMapper objectMapper) {
        return new RestAccessDeniedHandler(new ProblemDetailWriter(objectMapper));
    }

    /**
     * Turns the {@code role} claim into the authority {@code hasRole(...)} looks for.
     *
     * <p>Without it every {@code hasRole("ADMIN")} rule fails: the token verifies, the caller is
     * authenticated, and carries no authority whatsoever — so the answer is 403 for everyone, admin
     * included. The default converter reads {@code scope}/{@code scp} and prefixes with
     * {@code SCOPE_}, which is the OAuth2 convention for permissions granted to a <em>client</em>;
     * what the Bookland rules ask about is the role of the <em>person</em>.
     *
     * <p>A static factory and not a {@code @Bean}, deliberately. The resource-server configurer
     * looks a {@code JwtAuthenticationConverter} bean up from the context for <em>every</em> chain
     * that does not set one — checked in the bytecode of {@code JwtConfigurer} — so publishing it
     * would silently change the authorities on the Authorization Server's own chain as well. Each
     * API chain passes it explicitly instead.
     */
    public static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    /**
     * Copies the public half of every key out of the given source. The Authorization Server's
     * source also holds the private keys; the decoder never needs them, so it never gets them —
     * the same keys {@code /oauth2/jwks} publishes, read without the HTTP round trip.
     */
    private static JWKSource<SecurityContext> publicKeysOf(JWKSource<SecurityContext> source) {
        try {
            List<JWK> keys = source.get(new JWKSelector(new JWKMatcher.Builder().build()), null);
            return new ImmutableJWKSet<>(new JWKSet(keys).toPublicJWKSet());
        } catch (KeySourceException e) {
            throw new IllegalStateException("Could not read the token signing keys", e);
        }
    }

    private static void require(String value, String property) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("bookland.resource-server." + property + " is not set");
        }
    }
}
