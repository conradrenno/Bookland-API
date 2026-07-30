package com.devrenno.bookland.user.adapters.viewmodel;

import com.devrenno.bookland.user.domain.entity.UserRole;

import java.time.Instant;
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
        Instant createdAt,
        Instant updatedAt,
        boolean active
) {}
