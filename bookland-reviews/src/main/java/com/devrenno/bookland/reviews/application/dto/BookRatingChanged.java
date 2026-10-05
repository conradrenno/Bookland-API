package com.devrenno.bookland.reviews.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Event: a book's rating changed because a review was published or moderated away. Carries the
 * resulting average and count rather than the delta, so applying it twice leaves the same rating —
 * a consumer needs no bookkeeping to tolerate a redelivery.
 *
 * <p>This record is the reviews module's view of the event; the contract other modules code against
 * is the JSON it is serialized to, not this class.
 */
public record BookRatingChanged(
        UUID eventId,
        UUID bookId,
        double averageRating,
        int reviewCount,
        Instant occurredAt
) {
}
