package com.devrenno.bookland.reviews.infrastructure.web;

import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;
import com.devrenno.bookland.reviews.domain.exception.DuplicateReviewException;
import com.devrenno.bookland.reviews.domain.exception.PurchaseRequiredException;
import com.devrenno.bookland.reviews.domain.exception.ReviewAlreadyDeletedException;
import com.devrenno.bookland.reviews.domain.exception.ReviewNotFoundException;
import com.devrenno.bookland.websupport.ProblemDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ReviewExceptionHandler {

    @ExceptionHandler(ReviewNotFoundException.class)
    public ProblemDetail handleNotFound(ReviewNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(), "REVIEW_NOT_FOUND");
    }

    @ExceptionHandler(BookNotFoundException.class)
    public ProblemDetail handleBookNotFound(BookNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(), "BOOK_NOT_FOUND");
    }

    @ExceptionHandler(DuplicateReviewException.class)
    public ProblemDetail handleDuplicate(DuplicateReviewException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(), "DUPLICATE_REVIEW");
    }

    @ExceptionHandler(PurchaseRequiredException.class)
    public ProblemDetail handlePurchaseRequired(PurchaseRequiredException ex) {
        return ProblemDetails.of(HttpStatus.FORBIDDEN, ex.getMessage(), "PURCHASE_REQUIRED");
    }

    @ExceptionHandler(ReviewAlreadyDeletedException.class)
    public ProblemDetail handleAlreadyDeleted(ReviewAlreadyDeletedException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(), "REVIEW_ALREADY_DELETED");
    }
}
