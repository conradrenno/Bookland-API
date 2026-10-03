package com.devrenno.bookland.user.infrastructure.web;

import com.devrenno.bookland.user.domain.exception.AdminAccountDeactivationException;
import com.devrenno.bookland.user.domain.exception.EmailAlreadyExistsException;
import com.devrenno.bookland.user.domain.exception.UserAccessDeniedException;
import com.devrenno.bookland.user.domain.exception.UserNotFoundException;
import com.devrenno.bookland.websupport.ProblemDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Bean-validation failures are not handled here — {@code ValidationExceptionHandler} in
 * bookland-web-support owns them for the whole application, so that every module answers a rejected
 * payload with the same shape.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(), "USER_NOT_FOUND");
    }

    /**
     * A business 403: the token is fine and the role is irrelevant, the account simply is not the
     * caller's. Distinct from {@code INSUFFICIENT_ROLE} — see docs/error-contract.md.
     */
    @ExceptionHandler(UserAccessDeniedException.class)
    public ProblemDetail handleUserAccessDenied(UserAccessDeniedException ex) {
        return ProblemDetails.of(HttpStatus.FORBIDDEN, ex.getMessage(), "USER_ACCESS_DENIED");
    }

    @ExceptionHandler(AdminAccountDeactivationException.class)
    public ProblemDetail handleAdminAccountDeactivation(AdminAccountDeactivationException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(), "ADMIN_ACCOUNT_NOT_DELETABLE");
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(), "EMAIL_ALREADY_EXISTS");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, ex.getMessage(), "INVALID_ARGUMENT");
    }
}
