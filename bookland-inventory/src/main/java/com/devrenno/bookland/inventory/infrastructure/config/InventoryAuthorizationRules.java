package com.devrenno.bookland.inventory.infrastructure.config;

import com.devrenno.bookland.websupport.security.AuthorizationRules;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * Inventory is admin-only in its entirety: adjusting stock, reading the ledger, the low-stock
 * report. Its routes under {@code /api/v1/books} carry the {@code /inventory} suffix, so they cannot
 * meet the catalog's public {@code GET /api/v1/books/*}.
 */
@Component
public class InventoryAuthorizationRules implements AuthorizationRules {

    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rules) {
        rules.requestMatchers("/api/v1/books/*/inventory/**", "/api/v1/inventory/**").hasRole("ADMIN");
    }
}
