package com.devrenno.bookland.catalog.infrastructure.config;

import com.devrenno.bookland.websupport.security.AuthorizationRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * The catalog's departures from "authenticated by default": browsing is public, writing is admin.
 *
 * <p>Every pattern under {@code /api/v1/books} stops at {@code /api/v1/books/*} or names its suffix.
 * Inventory and reviews also have routes under that prefix; a {@code /api/v1/books/**} here would
 * match theirs too — which is exactly how review moderation used to be admin-only by accident,
 * through this module's {@code DELETE /api/v1/books/**}.
 */
@Component
public class CatalogAuthorizationRules implements AuthorizationRules {

    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rules) {
        rules
                .requestMatchers(HttpMethod.GET, "/api/v1/books", "/api/v1/books/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/categories", "/api/v1/categories/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/books", "/api/v1/books/*/cover").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/books/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/books/*").hasRole("ADMIN");
    }
}
