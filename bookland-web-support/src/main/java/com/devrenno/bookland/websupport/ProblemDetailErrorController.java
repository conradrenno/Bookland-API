package com.devrenno.bookland.websupport;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Renders the servlet container's error dispatch as problem+json.
 *
 * <p>Anything a {@code @RestControllerAdvice} does not handle — an unhandled exception escaping a
 * handler, a request matching no route at all — is forwarded by the container to {@code /error}.
 * Boot's {@code BasicErrorController} answers that with its own JSON shape ({@code timestamp},
 * {@code error}, {@code path}) and no {@code code}, which is the one hole left in the contract: a
 * client meets a body it has no type for exactly when something has gone wrong.
 *
 * <p>Two details matter to a client. {@code instance} is the path that actually failed, not
 * {@code /error} — a body claiming to describe {@code /error} tells the caller nothing about which
 * request broke. And a 5xx never carries the exception's own message: that is where stack traces,
 * SQL and column names leak.
 */
@RestController
public class ProblemDetailErrorController implements ErrorController {

    @RequestMapping("${server.error.path:${error.path:/error}}")
    public ResponseEntity<ProblemDetail> handleError(HttpServletRequest request) {
        HttpStatus status = resolveStatus(request);

        ProblemDetail problem = ProblemDetails.of(status, detailFor(status), codeFor(status));
        problem.setInstance(URI.create(originalPath(request)));

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private HttpStatus resolveStatus(HttpServletRequest request) {
        Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (code instanceof Integer statusCode) {
            HttpStatus resolved = HttpStatus.resolve(statusCode);
            if (resolved != null) {
                return resolved;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /** The request that failed, recorded by the container before it forwarded here. */
    private String originalPath(HttpServletRequest request) {
        Object uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        return uri instanceof String path ? path : request.getRequestURI();
    }

    private String codeFor(HttpStatus status) {
        return status.is5xxServerError() ? "INTERNAL_ERROR" : status.name();
    }

    private String detailFor(HttpStatus status) {
        return status.is5xxServerError()
                ? "The server failed to process the request"
                : status.getReasonPhrase();
    }
}
