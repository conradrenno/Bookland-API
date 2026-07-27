package com.devrenno.bookland.websupport.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

/**
 * The authentication/authorization half of the API error contract.
 *
 * <p>401 and 403 mean different things to a client and must never be conflated: a 401 tells it the
 * credential is missing or no longer good (refresh, then retry — or send the user to login), a 403
 * tells it the credential is fine but the role is not (show a forbidden screen; retrying is
 * pointless). The authenticating filter records why a token was rejected under
 * {@link #REQUEST_ATTRIBUTE}, since by the time the request is denied the exception is long gone.
 */
public enum AuthErrorCode {

    /** No credential was presented at all. */
    TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "Authentication is required to access this resource"),

    /** Well-formed and correctly signed, but past its expiration — the client should refresh. */
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "The access token has expired"),

    /** Malformed, badly signed or otherwise unusable — refreshing will not help. */
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "The access token is invalid"),

    /** Authenticated, but the granted authorities do not cover this resource. */
    INSUFFICIENT_ROLE(HttpStatus.FORBIDDEN, "Access is denied for the current role");

    public static final String REQUEST_ATTRIBUTE = "bookland.auth.error";

    private final HttpStatus status;
    private final String detail;

    AuthErrorCode(HttpStatus status, String detail) {
        this.status = status;
        this.detail = detail;
    }

    public HttpStatus status() {
        return status;
    }

    public String detail() {
        return detail;
    }

    /**
     * The rejection reason recorded by the authenticating filter, defaulting to
     * {@link #TOKEN_MISSING} — no attribute means no credential ever reached the filter.
     */
    public static AuthErrorCode fromRequest(HttpServletRequest request) {
        Object recorded = request.getAttribute(REQUEST_ATTRIBUTE);
        return recorded instanceof AuthErrorCode code ? code : TOKEN_MISSING;
    }
}
