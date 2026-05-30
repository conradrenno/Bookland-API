package com.devrenno.bookland.user.api.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserApiResponse(
        UUID id,
        String name,
        String email,
        String role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean active
) {}
