package com.devrenno.bookland.auth.application.dto;

import com.devrenno.bookland.user.domain.entity.UserRole;

import java.util.UUID;

public record AuthUserDto(
        UUID id,
        String email,
        String passwordHash,
        UserRole role
) {}
