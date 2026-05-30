package com.devrenno.bookland.auth.application.dto;

import java.util.UUID;

public record AuthUserDto(
        UUID id,
        String email,
        String passwordHash,
        String role
) {}
