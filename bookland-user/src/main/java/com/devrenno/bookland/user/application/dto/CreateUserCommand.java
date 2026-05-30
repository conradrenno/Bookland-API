package com.devrenno.bookland.user.application.dto;

import com.devrenno.bookland.user.domain.entity.UserRole;

public record CreateUserCommand(
        String name,
        String email,
        String rawPassword,
        UserRole role
) {}
