package com.devrenno.bookland.auth.infrastructure.config;

import com.devrenno.bookland.auth.infrastructure.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
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
                        // Order admin routes (must be before the broad authenticated cart/orders rules)
                        .requestMatchers("/api/v1/admin/orders/**").hasRole("ADMIN")
                        // Cart and order routes (authenticated customers)
                        .requestMatchers("/api/v1/cart/**").authenticated()
                        .requestMatchers("/api/v1/orders/**").authenticated()
                        // Payment routes
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/payments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/payments/**").authenticated()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**",
                                "/api-docs.yaml"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .headers(headers ->
                        headers.frameOptions(frame -> frame.sameOrigin()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
