package com.devrenno.bookland.websupport.validation;

import com.devrenno.bookland.websupport.ProblemDetails;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * The one place that turns a rejected request into a 400.
 *
 * <p>Declared once for the whole application on purpose: an advice per module means the payload
 * shape drifts apart module by module, and two advices handling the same exception leaves which one
 * wins up to bean ordering.
 *
 * <p>Validation failures carry a {@code errors} map (field → messages) on top of {@code detail}, so
 * a client can put each message next to its own form input instead of parsing a sentence.
 * {@code detail} stays populated for use as a summary banner.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ValidationExceptionHandler {

    private static final String VALIDATION_ERROR = "VALIDATION_ERROR";

    /** {@code @Valid} on a request body. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleInvalidBody(MethodArgumentNotValidException ex) {
        FieldErrors errors = new FieldErrors();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.add(error.getField(), message(error)));
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> errors.add(FieldErrors.OBJECT_LEVEL, message(error)));
        return validationProblem(errors);
    }

    /** Constraints on individual handler parameters (query string, path variables). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleInvalidParameters(HandlerMethodValidationException ex) {
        FieldErrors errors = new FieldErrors();
        ex.getParameterValidationResults().forEach(result -> {
            String parameterName = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(error -> errors.add(
                    error instanceof FieldError fieldError ? fieldError.getField() : parameterName,
                    message(error)));
        });
        // Constraints spanning more than one parameter belong to no single field.
        ex.getCrossParameterValidationResults()
                .forEach(error -> errors.add(FieldErrors.OBJECT_LEVEL, message(error)));
        return validationProblem(errors);
    }

    /** Constraints evaluated outside the handler signature (e.g. a validated component). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        FieldErrors errors = new FieldErrors();
        ex.getConstraintViolations().forEach(violation -> {
            String path = violation.getPropertyPath().toString();
            errors.add(path.substring(path.lastIndexOf('.') + 1), violation.getMessage());
        });
        return validationProblem(errors);
    }

    /** A path variable or query parameter that could not be converted (a malformed UUID, say). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Class<?> required = ex.getRequiredType();
        String expected = required != null ? required.getSimpleName() : "the expected type";

        FieldErrors errors = new FieldErrors();
        errors.add(ex.getName(), "must be a valid " + expected);

        ProblemDetail problem = ProblemDetails.of(HttpStatus.BAD_REQUEST,
                "'" + ex.getName() + "' is not a valid " + expected, "INVALID_PARAMETER");
        problem.setProperty(ProblemDetails.ERRORS, errors.asMap());
        return problem;
    }

    /**
     * Body absent, truncated or not parseable as JSON. The parser's own message names internal
     * types and byte offsets, so it is not passed through.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST,
                "The request body is missing or is not valid JSON", "MALFORMED_REQUEST");
    }

    private ProblemDetail validationProblem(FieldErrors errors) {
        ProblemDetail problem = ProblemDetails.of(HttpStatus.BAD_REQUEST, errors.summary(), VALIDATION_ERROR);
        problem.setProperty(ProblemDetails.ERRORS, errors.asMap());
        return problem;
    }

    private String message(MessageSourceResolvable error) {
        String message = error.getDefaultMessage();
        return message != null ? message : "is invalid";
    }
}
