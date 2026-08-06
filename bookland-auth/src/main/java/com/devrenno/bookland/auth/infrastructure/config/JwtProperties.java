package com.devrenno.bookland.auth.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "bookland.jwt")
public class JwtProperties {
    private String secret;
    /** 15 min. A stateless access token cannot be revoked at logout, so its TTL is the exposure window. */
    private long expirationMs = 900000L;
    private long refreshTokenExpirationMs = 604800000L;
}
