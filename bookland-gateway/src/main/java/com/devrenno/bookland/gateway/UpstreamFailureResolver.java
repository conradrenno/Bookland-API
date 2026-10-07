package com.devrenno.bookland.gateway;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpTimeoutException;

/**
 * Turns "the service behind me did not answer" into the error contract every service speaks
 * ({@code docs/error-contract.md}): problem+json with a {@code code}. Without it the gateway answered a
 * timeout with Boot's generic 500 (timestamp/error/path, no code) — measured in step 5d — the one
 * error format no client of this API ever sees from a service, and a 500 that reads as a bug.
 *
 * <ul>
 *   <li>no answer within the read or connect timeout → <b>504 UPSTREAM_TIMEOUT</b>;</li>
 *   <li>could not talk to the service at all (refused, closed, unresolvable) → <b>502
 *       UPSTREAM_UNAVAILABLE</b>.</li>
 * </ul>
 *
 * <p>A {@link HandlerExceptionResolver} rather than an {@code @ExceptionHandler}: the gateway's routes
 * are functional endpoints, not controller methods, and a resolver applies to whatever handler
 * failed. Anything else is left to the default handling (returns null).
 */
@Component
public class UpstreamFailureResolver implements HandlerExceptionResolver, Ordered {

    private static final Logger log = LoggerFactory.getLogger(UpstreamFailureResolver.class);

    private final JsonMapper jsonMapper;

    public UpstreamFailureResolver(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler,
                                         Exception exception) {
        HttpStatus status;
        String code;
        String detail;
        if (causedBy(exception, HttpTimeoutException.class)) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            code = "UPSTREAM_TIMEOUT";
            detail = "The service did not answer in time";
        } else if (causedBy(exception, IOException.class)) {
            status = HttpStatus.BAD_GATEWAY;
            code = "UPSTREAM_UNAVAILABLE";
            detail = "The service could not be reached";
        } else {
            return null;
        }
        log.warn("{} {} -> {} {}: {}", request.getMethod(), request.getRequestURI(), status.value(), code,
                rootCause(exception));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        try {
            response.setStatus(status.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            // Bytes, not getWriter(): through the writer the container appends its default charset
            // (ISO-8859-1, measured) to the content type of a JSON body.
            response.getOutputStream().write(jsonMapper.writeValueAsBytes(problem));
        } catch (IOException e) {
            return null;
        }
        return new ModelAndView();
    }

    /** Before Spring's own resolvers, which would otherwise turn these into a plain 500. */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private static boolean causedBy(Throwable throwable, Class<? extends Throwable> type) {
        for (Throwable t = throwable; t != null; t = t.getCause()) {
            if (type.isInstance(t)) {
                return true;
            }
        }
        return false;
    }

    private static String rootCause(Throwable throwable) {
        Throwable t = throwable;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t.getClass().getSimpleName() + ": " + t.getMessage();
    }
}
