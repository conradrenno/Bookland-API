package com.devrenno.bookland.auth.domain.exception;

/**
 * A token that could not be parsed, verified or trusted — as opposed to
 * {@link TokenExpiredException}, which is a valid token past its lifetime. The distinction is what
 * lets a client know whether refreshing is worth attempting.
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(Throwable cause) {
        super("Token is invalid", cause);
    }
}
