package com.devrenno.bookland.user.adapters.viewmodel;

import com.devrenno.bookland.user.domain.entity.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Output model produced by the Presenter and delivered as-is by the HTTP layer.
 * Framework-free: no Jackson annotations, and intentionally omits passwordHash.
 */
public record UserViewModel(
        UUID id,
        String name,
        String email,
        UserRole role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean active
) {}
