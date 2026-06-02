package com.devrenno.bookland.reviews.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID bookId,
        UUID customerId,
        int rating,
        String comment,
        LocalDateTime createdAt
) {}
