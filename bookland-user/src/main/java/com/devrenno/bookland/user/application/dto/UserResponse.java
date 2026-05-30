package com.devrenno.bookland.user.application.dto;

import com.devrenno.bookland.user.domain.entity.UserRole;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        UserRole role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean active,
        @JsonIgnore String passwordHash
) {}
