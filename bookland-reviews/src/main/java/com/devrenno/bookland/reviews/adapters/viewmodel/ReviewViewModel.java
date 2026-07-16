package com.devrenno.bookland.reviews.adapters.viewmodel;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewViewModel(
        UUID id,
        UUID bookId,
        UUID customerId,
        int rating,
        String comment,
        LocalDateTime createdAt
) {}
