package com.devrenno.bookland.catalog.infrastructure.web;

import com.devrenno.bookland.catalog.domain.exception.BookHasActiveOrdersException;
import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;
import com.devrenno.bookland.catalog.domain.exception.CategoryNotFoundException;
import com.devrenno.bookland.catalog.domain.exception.InsufficientStockException;
import com.devrenno.bookland.catalog.domain.exception.InvalidImageException;
import com.devrenno.bookland.catalog.domain.exception.IsbnAlreadyExistsException;
import com.devrenno.bookland.websupport.ProblemDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Bean-validation failures are not handled here — {@code ValidationExceptionHandler} in
 * bookland-web-support owns them for the whole application, so that every module answers a rejected
 * payload with the same shape.
 */
@RestControllerAdvice
public class CatalogExceptionHandler {

    @ExceptionHandler(BookNotFoundException.class)
    public ProblemDetail handleBookNotFound(BookNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(), "BOOK_NOT_FOUND");
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ProblemDetail handleCategoryNotFound(CategoryNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(), "CATEGORY_NOT_FOUND");
    }

    @ExceptionHandler(IsbnAlreadyExistsException.class)
    public ProblemDetail handleIsbnAlreadyExists(IsbnAlreadyExistsException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(), "ISBN_ALREADY_EXISTS");
    }

    @ExceptionHandler(BookHasActiveOrdersException.class)
    public ProblemDetail handleBookHasActiveOrders(BookHasActiveOrdersException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(), "BOOK_HAS_ACTIVE_ORDERS");
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStock(InsufficientStockException ex) {
        return ProblemDetails.of(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), "INSUFFICIENT_STOCK");
    }

    @ExceptionHandler(InvalidImageException.class)
    public ProblemDetail handleInvalidImage(InvalidImageException ex) {
        return ProblemDetails.of(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), "INVALID_IMAGE");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ProblemDetails.of(HttpStatus.PAYLOAD_TOO_LARGE,
                "Uploaded file exceeds the maximum allowed size", "FILE_TOO_LARGE");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, ex.getMessage(), "INVALID_ARGUMENT");
    }
}
