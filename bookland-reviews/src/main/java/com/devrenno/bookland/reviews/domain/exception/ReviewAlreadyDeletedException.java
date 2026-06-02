package com.devrenno.bookland.reviews.domain.exception;

import java.util.UUID;

public class ReviewAlreadyDeletedException extends RuntimeException {
    public ReviewAlreadyDeletedException(UUID reviewId) {
        super("Review " + reviewId + " has already been deleted");
    }
}
