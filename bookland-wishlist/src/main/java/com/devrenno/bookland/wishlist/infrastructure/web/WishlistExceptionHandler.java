package com.devrenno.bookland.wishlist.infrastructure.web;

import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;
import com.devrenno.bookland.wishlist.domain.exception.WishlistItemAlreadyExistsException;
import com.devrenno.bookland.wishlist.domain.exception.WishlistItemNotFoundException;
import com.devrenno.bookland.websupport.ProblemDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class WishlistExceptionHandler {

    @ExceptionHandler(WishlistItemNotFoundException.class)
    public ProblemDetail handleNotFound(WishlistItemNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(), "WISHLIST_ITEM_NOT_FOUND");
    }

    @ExceptionHandler(WishlistItemAlreadyExistsException.class)
    public ProblemDetail handleAlreadyExists(WishlistItemAlreadyExistsException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(), "WISHLIST_ITEM_ALREADY_EXISTS");
    }

    @ExceptionHandler(BookNotFoundException.class)
    public ProblemDetail handleBookNotFound(BookNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(), "BOOK_NOT_FOUND");
    }
}
