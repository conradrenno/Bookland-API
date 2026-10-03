package com.devrenno.bookland.reviews.domain.entity;

import com.devrenno.bookland.reviews.domain.exception.ReviewAlreadyDeletedException;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class Review {

    private final UUID id;
    private final UUID bookId;
    private final UUID customerId;
    /**
     * The author's display name as it was when the review was written. Stored rather than looked up
     * on every listing, so that listing reviews does not depend on the user module being reachable.
     * Null on reviews whose author could not be resolved when the column was introduced.
     */
    private final String customerName;
    private final int rating;
    private final String comment;
    private final Instant createdAt;
    private boolean deleted;

    private Review(UUID id, UUID bookId, UUID customerId, String customerName, int rating, String comment,
                   Instant createdAt, boolean deleted) {
        this.id = id;
        this.bookId = bookId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
        this.deleted = deleted;
    }

    public static Review create(UUID bookId, UUID customerId, String customerName, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("rating must be between 1 and 5");
        }
        return new Review(UUID.randomUUID(), bookId, customerId, customerName, rating, comment, Instant.now(), false);
    }

    public static Review reconstitute(UUID id, UUID bookId, UUID customerId, String customerName, int rating,
                                      String comment, Instant createdAt, boolean deleted) {
        return new Review(id, bookId, customerId, customerName, rating, comment, createdAt, deleted);
    }

    public void softDelete() {
        if (deleted) throw new ReviewAlreadyDeletedException(id);
        this.deleted = true;
    }
}
