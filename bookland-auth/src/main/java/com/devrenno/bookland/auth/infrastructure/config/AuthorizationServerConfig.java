package com.devrenno.bookland.auth.infrastructure.config;

import com.devrenno.bookland.auth.application.port.out.UserLookupPort;
import com.devrenno.bookland.auth.infrastructure.security.AuthorizationJsonMapperFactory;
import com.devrenno.bookland.auth.infrastructure.security.BooklandTokenCustomizer;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
// Both this and OAuth2AuthorizationServerConfigurer moved out of the Authorization Server jar into
// spring-security-config when the project joined the Spring Security release train. Every import
// found in material written for the 1.x line points at the old package and will not resolve.
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import tools.jackson.databind.json.JsonMapper;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

/**
 * The Authorization Server: its filter chain, its signing key, its persistence.
 *
 * <p>Four chains live in this application and the order is load-bearing. This one is first and
 * claims only the protocol endpoints, through the matcher the configurer itself publishes. The login
 * form is second. The API chains — {@code ApiSecurityConfig} in bookland-web-support — come last and
 * catch everything else, validating the tokens issued here against the public half of the same key. The decoder that does
 * the validating is not here: it is {@code ResourceServerConfig} in bookland-web-support, so that a
 * service outside this process can validate tokens without depending on the Authorization Server.
 *
 * <p>The first two chains are stateful, holding an HTTP session, while the API chain stays
 * {@code STATELESS}. Two session models coexisting is not an accident to be tidied up later; it is
 * the reason there is more than one chain. A browser logging in has a session; an API client
 * presenting a Bearer token must not.
 */
@Configuration
public class AuthorizationServerConfig {

    /** The framework's default; the CORS rule and the preflight matcher below must name the same path. */
    private static final String TOKEN_ENDPOINT = "/oauth2/token";

    /**
     * Only the OAuth2/OIDC endpoints. {@code getEndpointsMatcher()} covers /oauth2/authorize,
     * /oauth2/token, /oauth2/jwks, /userinfo and the two discovery documents — so the routes never
     * have to be listed by hand and cannot drift from what the configurer actually installs.
     *
     * <p>Discovery and JWKS answer publicly despite {@code anyRequest().authenticated()}: their
     * filters sit ahead of the authorization filter and write the response without continuing down
     * the chain. That is by design — a JWKS publishes the public half of the signing key, which is
     * what lets a resource server verify a token without holding anything that could forge one.
     *
     * <p>CSRF is disabled for these endpoints because the token endpoint is a back-channel POST
     * carrying client credentials, not a browser form.
     *
     * <p>{@code oauth2ResourceServer} on this chain is not a copy-paste artefact: with OIDC enabled,
     * {@code /userinfo} is itself a resource protected by an access token, so the Authorization
     * Server has to validate the tokens it issues. Leave it out and /userinfo answers 401.
     *
     * <p>CORS on the token endpoint, for the one caller that exchanges a code from a browser page
     * served by another origin: the API's Swagger UI, now that the API and this server listen on
     * different ports. Origins are listed in {@code bookland.oauth2.cors-allowed-origins}; with the
     * list empty no origin is allowed, which is what a server reached only through redirects and
     * back-channel calls should answer.
     *
     * <p>The chain has to claim the preflight itself. The configurer matches its token endpoint as
     * {@code POST /oauth2/token} only (read in the bytecode of {@code OAuth2TokenEndpointConfigurer}),
     * so the browser's {@code OPTIONS} fell through to the API chain and was answered 401 before any
     * CORS rule was consulted.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerFilterChain(HttpSecurity http,
                                                              AuthorizationServerProperties properties) throws Exception {
        // Plain constructor: the static authorizationServer() factory that 1.3-era samples use is
        // not part of this version's API.
        OAuth2AuthorizationServerConfigurer authorizationServer = new OAuth2AuthorizationServerConfigurer();

        return http
                .securityMatcher(new OrRequestMatcher(
                        authorizationServer.getEndpointsMatcher(),
                        PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.OPTIONS, TOKEN_ENDPOINT)))
                .with(authorizationServer, server -> server.oidc(Customizer.withDefaults()))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers(authorizationServer.getEndpointsMatcher()))
                .cors(cors -> cors.configurationSource(tokenEndpointCors(properties.getCorsAllowedOrigins())))
                // A browser arriving at /oauth2/authorize without a session must be sent to the form,
                // not handed the API's problem+json 401. The media type is what separates the two.
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))
                .build();
    }

    /**
     * Only {@code /oauth2/token}: {@code /oauth2/authorize} is a navigation, which CORS does not
     * govern, and nothing else here is fetched from a browser page.
     */
    static CorsConfigurationSource tokenEndpointCors(List<String> allowedOrigins) {
        CorsConfiguration token = new CorsConfiguration();
        token.setAllowedOrigins(allowedOrigins);
        token.setAllowedMethods(List.of("POST"));
        token.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(TOKEN_ENDPOINT, token);
        return source;
    }

    /**
     * The login form, on its own chain because the API chain matches everything and is stateless —
     * a form login there would have nowhere to keep the result.
     *
     * <p>Spring Security's generated page is deliberate for now. Seeing the default form appear is
     * the proof that the authorization code flow reached the authentication step; a styled page
     * belongs with the BFF, which is what will own the login experience.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain loginFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/login", "/login/**")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .formLogin(Customizer.withDefaults())
                .build();
    }

    /** What {@code DaoAuthenticationProvider} checks the login form's password with. */
    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Signs every token. Read from configuration rather than generated per boot: a fresh pair on
     * each restart logs everyone out and cannot work behind more than one instance, and Phase 2
     * needs the key to outlive a single process anyway.
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource(AuthorizationServerProperties properties) {
        RSAPublicKey publicKey = readPublicKey(properties.getJwk().getPublicKey());
        RSAPrivateKey privateKey = readPrivateKey(properties.getJwk().getPrivateKey());

        RSAKey key = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(properties.getJwk().getKeyId())
                .build();

        return new ImmutableJWKSet<>(new JWKSet(key));
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer(AuthorizationServerProperties properties,
                                                                     UserLookupPort userLookupPort) {
        return new BooklandTokenCustomizer(properties.getApiAudience(), userLookupPort);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(AuthorizationServerProperties properties) {
        return AuthorizationServerSettings.builder()
                .issuer(properties.getIssuer())
                .build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    /**
     * Persisted, so an authorization survives a restart and Phase 2 finds it already shared.
     *
     * <p>The two mappers are replaced only to hand them a JSON mapper that knows
     * {@code BooklandUserDetails} — everything else about them is the default. Note what they are
     * <em>not</em>: the 1.x-era {@code setObjectMapper} taking a Jackson 2 {@code ObjectMapper}
     * still exists on this class and still works, and using it on Boot 4 quietly moves the whole
     * service onto the Jackson 2 path. The constructor here already defaults to Jackson 3.
     */
    @Bean
    public OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate,
                                                           RegisteredClientRepository registeredClientRepository) {
        JsonMapper jsonMapper = AuthorizationJsonMapperFactory.create();

        var rowMapper = new JdbcOAuth2AuthorizationService.JsonMapperOAuth2AuthorizationRowMapper(
                registeredClientRepository, jsonMapper);
        // The constructor we are bypassing sets this; the row mapper reads every text column
        // through it and fails with a null pointer without one.
        rowMapper.setLobHandler(new DefaultLobHandler());

        JdbcOAuth2AuthorizationService service =
                new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
        service.setAuthorizationRowMapper(rowMapper);
        service.setAuthorizationParametersMapper(
                new JdbcOAuth2AuthorizationService.JsonMapperOAuth2AuthorizationParametersMapper(jsonMapper));
        return service;
    }

    /**
     * In memory: a first-party client with consent turned off never writes a row. The table exists
     * so that a third-party client later needs no migration.
     */
    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService() {
        return new InMemoryOAuth2AuthorizationConsentService();
    }

    private RSAPublicKey readPublicKey(String base64Der) {
        try {
            var spec = new X509EncodedKeySpec(Base64.getDecoder().decode(base64Der));
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalStateException("bookland.oauth2.jwk.public-key is not a base64 X.509 RSA key", e);
        }
    }

    private RSAPrivateKey readPrivateKey(String base64Der) {
        try {
            var spec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64Der));
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException("bookland.oauth2.jwk.private-key is not a base64 PKCS#8 RSA key", e);
        }
    }
}
