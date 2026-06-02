package com.devrenno.bookland.reviews.application.dto;

import java.util.UUID;

public record CreateReviewCommand(UUID bookId, UUID customerId, int rating, String comment) {}
