package com.devrenno.bookland.auth.application.dto;

import java.time.Instant;

public record TokenResponse(
        String accessToken,
        String tokenType,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {
    public static TokenResponse bearer(String accessToken, Instant accessTokenExpiresAt,
                                        String refreshToken, Instant refreshTokenExpiresAt) {
        return new TokenResponse(accessToken, "Bearer", accessTokenExpiresAt, refreshToken, refreshTokenExpiresAt);
    }
}
