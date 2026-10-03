package com.devrenno.bookland.reviews.infrastructure.config;

import com.devrenno.bookland.websupport.security.AuthorizationRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * Reviews are public to read and admin-only to moderate. Writing one needs only an authenticated
 * caller, which is the default, so it is not declared.
 *
 * <p>Both rules used to be implied by the catalog's {@code /api/v1/books/**} patterns. The
 * moderation route was admin-only only because the catalog's {@code DELETE /api/v1/books/**}
 * happened to match it: narrowing that rule would have opened moderation to every customer, with no
 * line in this module to show it.
 */
@Component
public class ReviewsAuthorizationRules implements AuthorizationRules {

    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rules) {
        rules
                .requestMatchers(HttpMethod.GET, "/api/v1/books/*/reviews").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/api/v1/books/*/reviews/*").hasRole("ADMIN");
    }
}
