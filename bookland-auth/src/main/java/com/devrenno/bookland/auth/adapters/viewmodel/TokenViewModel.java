package com.devrenno.bookland.auth.adapters.viewmodel;

import java.time.Instant;

/**
 * Delivery-facing output model for the issued token pair. Framework-free (no Jackson).
 */
public record TokenViewModel(
        String accessToken,
        String tokenType,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {}
