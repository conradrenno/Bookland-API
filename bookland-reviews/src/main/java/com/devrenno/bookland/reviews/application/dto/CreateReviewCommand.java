package com.devrenno.bookland.reviews.application.dto;

import java.util.UUID;

/**
 * @param customerName the author's display name as the access token carries it — stored on the review
 *                     so that listing reviews asks nobody. Null when the token carries none.
 */
public record CreateReviewCommand(UUID bookId, UUID customerId, String customerName, int rating, String comment) {}
