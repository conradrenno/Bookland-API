package com.devrenno.bookland.orders.infrastructure.web;

import com.devrenno.bookland.orders.application.port.out.CatalogUnavailableException;
import com.devrenno.bookland.orders.domain.exception.BookNotFoundException;
import com.devrenno.bookland.orders.domain.exception.BookNotInCartException;
import com.devrenno.bookland.orders.domain.exception.CartItemUnavailableException;
import com.devrenno.bookland.orders.domain.exception.CartNotFoundException;
import com.devrenno.bookland.orders.domain.exception.CheckoutInProgressException;
import com.devrenno.bookland.orders.domain.exception.InvalidOrderStatusTransitionException;
import com.devrenno.bookland.orders.domain.exception.OrderAccessDeniedException;
import com.devrenno.bookland.orders.domain.exception.OrderCancellationNotAllowedException;
import com.devrenno.bookland.orders.domain.exception.OrderNotFoundException;
import com.devrenno.bookland.websupport.ProblemDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class OrderExceptionHandler {

    @ExceptionHandler(CartNotFoundException.class)
    public ProblemDetail handleCartNotFound(CartNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(), "CART_NOT_FOUND");
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail handleOrderNotFound(OrderNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(), "ORDER_NOT_FOUND");
    }

    @ExceptionHandler(OrderAccessDeniedException.class)
    public ProblemDetail handleAccessDenied(OrderAccessDeniedException ex) {
        return ProblemDetails.of(HttpStatus.FORBIDDEN, ex.getMessage(), "ORDER_ACCESS_DENIED");
    }

    /** The catalog's own handler used to answer this, being global in the monolith; no longer. */
    @ExceptionHandler(BookNotFoundException.class)
    public ProblemDetail handleBookNotFound(BookNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(), "BOOK_NOT_FOUND");
    }

    @ExceptionHandler(CatalogUnavailableException.class)
    public ProblemDetail handleCatalogUnavailable(CatalogUnavailableException ex) {
        return ProblemDetails.of(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), "CATALOG_UNAVAILABLE");
    }

    @ExceptionHandler(CartItemUnavailableException.class)
    public ProblemDetail handleCartItemUnavailable(CartItemUnavailableException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(), "CART_ITEM_UNAVAILABLE");
    }

    @ExceptionHandler(BookNotInCartException.class)
    public ProblemDetail handleBookNotInCart(BookNotInCartException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(), "BOOK_NOT_IN_CART");
    }

    @ExceptionHandler(OrderCancellationNotAllowedException.class)
    public ProblemDetail handleCancellationNotAllowed(OrderCancellationNotAllowedException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(), "ORDER_CANCELLATION_NOT_ALLOWED");
    }

    @ExceptionHandler(InvalidOrderStatusTransitionException.class)
    public ProblemDetail handleInvalidTransition(InvalidOrderStatusTransitionException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(), "INVALID_ORDER_STATUS_TRANSITION");
    }

    @ExceptionHandler(CheckoutInProgressException.class)
    public ProblemDetail handleCheckoutInProgress(CheckoutInProgressException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(), "CHECKOUT_IN_PROGRESS");
    }
}
