package com.devrenno.bookland.websupport.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * A module's exceptions to the API's default access rule, declared by the module that owns the
 * routes. The default — every route requires an authenticated caller, and everything under
 * {@code /api/v1/admin/**} requires an admin — is applied by whoever assembles the security chain,
 * after the rules of every module.
 *
 * <p>So a module declares only what departs from that default: the routes anyone may call, and the
 * admin routes that live outside {@code /api/v1/admin}. A module with neither declares nothing.
 *
 * <p><strong>A module may only match its own routes, and never with a {@code **} under a prefix it
 * shares with another module.</strong> The chain evaluates rules in order and stops at the first
 * match, but the order in which the modules' beans are collected is not defined. Rules from two
 * modules that can match the same request would make the outcome depend on that order. Rules that
 * cannot overlap make it irrelevant. {@code /api/v1/books} is the shared prefix today: catalog,
 * inventory and reviews all have routes under it, so each declares {@code /api/v1/books/*} with
 * its own suffix — {@code *} matches one path segment, {@code **} any number.
 *
 * <p>{@code AccessMatrixIntegrationTest} (bookland-app) pins the outcome for every route.
 */
@FunctionalInterface
public interface AuthorizationRules {

    void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rules);
}
