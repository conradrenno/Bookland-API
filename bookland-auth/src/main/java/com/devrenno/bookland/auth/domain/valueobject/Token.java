package com.devrenno.bookland.auth.domain.valueobject;

import java.time.Instant;
import java.util.UUID;

public record Token(
        String value,
        Instant expiresAt,
        UUID userId,
        String email,
        String role
) {}
