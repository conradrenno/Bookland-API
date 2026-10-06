package com.devrenno.bookland.websupport.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.util.List;

/**
 * The two chains every Bookland process serves its API with, as a resource server: chain 3 for the
 * routes that are not the API, chain 4 for the API itself.
 *
 * <p>They used to live in bookland-auth next to the Authorization Server's chains, which tied the
 * security of the whole API to the module that issues tokens: a process without bookland-auth had no
 * API chain at all. Here, any process that has this module validates tokens and applies its
 * modules' {@link AuthorizationRules} with the same two chains. Orders 1 and 2 are left to the
 * Authorization Server, in the one process that hosts it.
 */
@Configuration
@EnableWebSecurity
public class ApiSecurityConfig {

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
     *
     * <p>It owns no route-specific rule. Each module publishes its own exceptions to the default as
     * an {@link AuthorizationRules} bean, next to the controllers they protect, so that a module
     * extracted into a service takes its rules with it. What is left here is the default every
     * route falls back to.
     */
    @Bean
    @Order(4)
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   List<AuthorizationRules> moduleRules,
                                                   AuthenticationEntryPoint authenticationEntryPoint,
                                                   AccessDeniedHandler accessDeniedHandler) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    // Each module's own exceptions first. They cannot overlap (see AuthorizationRules),
                    // so the order in which the beans arrive does not matter.
                    moduleRules.forEach(rules -> rules.configure(auth));
                    auth
                            // The whole /admin prefix, so a new back-office controller is closed by
                            // default instead of falling through to anyRequest().authenticated()
                            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                            .anyRequest().authenticated();
                })
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
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(ResourceServerConfig.jwtAuthenticationConverter())))
                .build();
    }
}
