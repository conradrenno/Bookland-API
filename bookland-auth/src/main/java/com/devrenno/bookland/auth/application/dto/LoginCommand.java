package com.devrenno.bookland.auth.application.dto;

public record LoginCommand(
        String email,
        String rawPassword
) {}
