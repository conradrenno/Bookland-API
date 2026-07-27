package com.devrenno.bookland.websupport;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Builds the API's RFC 7807 responses.
 *
 * <p>Every error body carries a machine-readable {@code code} on top of the standard members, so
 * clients branch on a stable symbol instead of parsing {@code detail}. {@code title} and
 * {@code type} come for free from {@link ProblemDetail} (the status reason phrase and
 * {@code about:blank}); {@code instance} is filled in by Spring for bodies returned from a
 * {@code @RestControllerAdvice}, and by {@link ProblemDetailWriter} for the ones written straight
 * to the response inside the filter chain.
 */
public final class ProblemDetails {

    public static final String CODE = "code";

    /** Validation only: field name → the messages that field broke. */
    public static final String ERRORS = "errors";

    private ProblemDetails() {
    }

    public static ProblemDetail of(HttpStatus status, String detail, String code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty(CODE, code);
        return problem;
    }
}
