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
    private long expirationMs = 86400000L;
    private long refreshTokenExpirationMs = 604800000L;
}
