package com.devrenno.bookland.user.infrastructure.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bookland.admin")
public record AdminProperties(String email, String password) {}