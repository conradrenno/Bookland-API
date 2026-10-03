package com.devrenno.bookland.auth.infrastructure.config;

import com.devrenno.bookland.auth.infrastructure.security.BearerTokenErrorClassifier;
import com.devrenno.bookland.websupport.ProblemDetailWriter;
import com.devrenno.bookland.websupport.security.RestAccessDeniedHandler;
import com.devrenno.bookland.websupport.security.RestAuthenticationEntryPoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Routes that are not the API and must never be answered with a token error.
     *
     * <p>They are here rather than as {@code permitAll} rules in the API chain because of a change
     * the resource server brings: its filter rejects an unusable Bearer token outright, wherever it
     * appears, while the filter it replaces recorded the reason and let the request carry on. For
     * most routes that is an improvement. For {@code /error} it is a serious regression, and one
     * this project has already fixed once.
     *
     * <p>Boot runs the security chain on the {@code ERROR} dispatch too. So an unhandled exception
     * forwards to {@code /error} carrying the original request's headers — including the stale token
     * that has nothing to do with the failure — and the forward is answered with 401
     * {@code TOKEN_INVALID}. The real 500 never reaches the client, and arrives disguised as an
     * expired session: the one thing a client reacts to by refreshing and then signing the user out.
     * Verified against a running server, not deduced.
     *
     * <p>The same reasoning covers the console, the API document and stored images: none of them is
     * an API route, none can be confused with one, and none should stop working because the caller
     * happens to be holding a token that has gone bad.
     *
     * <p>Note what is deliberately <em>not</em> here: {@code /api/v1/books} and
     * {@code /api/v1/categories}. Their public reads share a path prefix with admin writes, so a
     * chain claiming them by path would have to reproduce the method-by-method rules below to avoid
     * opening a write route. That is the mistake worth avoiding, so those stay in the API chain
     * where the rules already live.
     */
    @Bean
    @Order(3)
    public SecurityFilterChain infrastructureFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(
                        "/error",
                        "/media/**",
                        "/h2-console/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/api-docs/**",
                        "/api-docs.yaml")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers ->
                        headers.frameOptions(frame -> frame.sameOrigin()))
                .build();
    }

    /**
     * Last of the four chains, and the only one without a {@code securityMatcher} — it catches
     * everything the previous three did not claim. The order is explicit rather than left to the
     * default lowest precedence, because "the API chain happens to sort last" is not something a
     * reader should have to work out.
     */
    @Bean
    @Order(4)
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationEntryPoint authenticationEntryPoint,
                                                   AccessDeniedHandler accessDeniedHandler) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                        // Inventory admin routes (must be before the broad GET permitAll for books)
                        .requestMatchers(HttpMethod.GET, "/api/v1/books/*/inventory/history").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/inventory/low-stock").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/books/*/inventory").hasRole("ADMIN")
                        // Catalog public routes
                        .requestMatchers(HttpMethod.GET, "/api/v1/books/**", "/api/v1/books").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**", "/api/v1/categories").permitAll()
                        // Cover images (static media)
                        .requestMatchers(HttpMethod.GET, "/media/**").permitAll()
                        // Catalog admin routes
                        .requestMatchers(HttpMethod.POST, "/api/v1/books/*/cover").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/books").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/books/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/books/**").hasRole("ADMIN")
                        // The whole /admin prefix, so a new back-office controller is closed by default
                        // instead of falling through to anyRequest().authenticated()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // Cart and order routes (authenticated customers)
                        .requestMatchers("/api/v1/cart/**").authenticated()
                        .requestMatchers("/api/v1/orders/**").authenticated()
                        // Payment routes. There is no admin payment route: a refund is half of a
                        // cancellation and is reached only through PATCH /admin/orders/{id}/status.
                        .requestMatchers(HttpMethod.GET, "/api/v1/payments/**").authenticated()
                        // The container forwards here after an unhandled exception, on a dispatch
                        // the security chain also filters. Left authenticated, it answers the
                        // forward with 401 TOKEN_MISSING and the real 500 never reaches the client
                        // — a server bug arriving disguised as an expired session, which is the
                        // one thing a client must not retry a refresh for.
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**",
                                "/api-docs.yaml"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                // Without these two the chain falls back to Http403ForbiddenEntryPoint, which
                // answers every denial — missing token, expired token, wrong role — with an empty
                // 403 that no client can act on.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .headers(headers ->
                        headers.frameOptions(frame -> frame.sameOrigin()))
                // Registering the entry point here as well is not redundant. The resource server
                // installs its own BearerTokenAuthenticationEntryPoint, which answers with a
                // WWW-Authenticate header and an empty body; configuring only .exceptionHandling()
                // above leaves that one in charge of every rejected token, and the problem+json
                // contract dies for exactly the responses a client most needs to read.
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    /**
     * Turns the {@code role} claim into the authority {@code hasRole(...)} looks for.
     *
     * <p>Without it every {@code hasRole("ADMIN")} rule above fails: the token verifies, the caller
     * is authenticated, and carries no authority whatsoever — so the answer is 403 for everyone,
     * admin included. The default converter reads {@code scope}/{@code scp} and prefixes with
     * {@code SCOPE_}, which is the OAuth2 convention for permissions granted to a <em>client</em>;
     * what the Bookland rules ask about is the role of the <em>person</em>, which the customizer
     * writes into {@code role}.
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    /**
     * The classifier wraps the renderer: it reads the rejection reason out of the exception chain
     * and records it, then lets {@code RestAuthenticationEntryPoint} write the body. That split is
     * what preserves {@code TOKEN_EXPIRED} vs {@code TOKEN_INVALID} now that the two {@code catch}
     * blocks of the old filter are gone.
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

    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
