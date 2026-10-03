package com.devrenno.bookland.auth.infrastructure.config;

import com.devrenno.bookland.websupport.security.AuthorizationRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * The auth module's departures from "authenticated by default". Only registration: the protocol
 * endpoints (/oauth2/**, /login, discovery) are not on the API chain at all, they have chains of
 * their own in {@code AuthorizationServerConfig}.
 */
@Component
public class AuthAuthorizationRules implements AuthorizationRules {

    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rules) {
        rules.requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll();
    }
}
