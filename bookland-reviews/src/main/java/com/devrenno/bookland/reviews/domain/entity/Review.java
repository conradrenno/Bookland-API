package com.devrenno.bookland.reviews.domain.entity;

import com.devrenno.bookland.reviews.domain.exception.ReviewAlreadyDeletedException;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class Review {

    private final UUID id;
    private final UUID bookId;
    private final UUID customerId;
    private final int rating;
    private final String comment;
    private final LocalDateTime createdAt;
    private boolean deleted;

    public static Review create(UUID bookId, UUID customerId, int rating, String comment) {
        return Review.builder()
                .id(UUID.randomUUID())
                .bookId(bookId)
                .customerId(customerId)
                .rating(rating)
                .comment(comment)
                .createdAt(LocalDateTime.now())
                .deleted(false)
                .build();
    }

    public void softDelete() {
        if (deleted) throw new ReviewAlreadyDeletedException(id);
        this.deleted = true;
    }
}
