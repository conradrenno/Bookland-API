package com.devrenno.bookland;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bookland.admin")
public record AdminProperties(String email, String password) {}