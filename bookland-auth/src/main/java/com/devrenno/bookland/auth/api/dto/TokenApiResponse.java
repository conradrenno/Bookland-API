package com.devrenno.bookland.auth.api.dto;

import java.time.Instant;

public record TokenApiResponse(
        String accessToken,
        String tokenType,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {}
