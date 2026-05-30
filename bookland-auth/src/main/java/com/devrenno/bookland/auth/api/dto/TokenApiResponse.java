package com.devrenno.bookland.auth.api.dto;

import java.time.Instant;

public record TokenApiResponse(
        String token,
        String tokenType,
        Instant expiresAt
) {}
