package com.devrenno.bookland.reviews.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Query read-model: a review labelled with its author's display name (null when the customer
 * can no longer be resolved — the delivery layer decides how to render that).
 */
public record ReviewView(
        UUID id,
        UUID bookId,
        UUID customerId,
        String customerName,
        int rating,
        String comment,
        LocalDateTime createdAt
) {}
