package com.devrenno.bookland.auth.application.dto;

public record RegisterCommand(
        String name,
        String email,
        String rawPassword
) {}
