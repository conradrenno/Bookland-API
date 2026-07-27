package com.devrenno.bookland.websupport.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;

import java.util.List;

/**
 * Publishes the error contract in the OpenAPI document.
 *
 * <p>Springdoc only documents what a handler returns, so a generated document describes the 200
 * path and nothing else — leaving every client to hand-write the error type it will certainly meet.
 * This adds it: a {@code default} response on every operation, plus an explicit 400 with the
 * {@code errors} map wherever a request carries a body or parameters to reject.
 *
 * <p>Statuses are not enumerated per operation on purpose. Which of 401/403/404/409/422 an endpoint
 * can produce is decided by security rules and domain exceptions that no annotation on the handler
 * knows about, so a hand-maintained list would go stale silently; {@code default} is both accurate
 * and enough for a generator to produce one error type.
 */
public class ErrorResponsesCustomizer implements OpenApiCustomizer {

    public static final String PROBLEM_DETAIL = "ProblemDetail";
    public static final String VALIDATION_PROBLEM_DETAIL = "ValidationProblemDetail";

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String SCHEMA_REF = "#/components/schemas/";

    @Override
    public void customise(OpenAPI openApi) {
        registerSchemas(openApi);

        if (openApi.getPaths() == null) {
            return;
        }
        openApi.getPaths().values().forEach(pathItem ->
                pathItem.readOperations().forEach(this::describeErrors));
    }

    private void registerSchemas(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        openApi.getComponents()
                .addSchemas(PROBLEM_DETAIL, problemDetail())
                .addSchemas(VALIDATION_PROBLEM_DETAIL, validationProblemDetail());
    }

    private void describeErrors(Operation operation) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }

        if (responses.getDefault() == null) {
            responses.addApiResponse(ApiResponses.DEFAULT,
                    response("Error response", PROBLEM_DETAIL));
        }
        if (rejectsInput(operation) && responses.get("400") == null) {
            responses.addApiResponse("400",
                    response("The request was rejected before reaching the handler",
                            VALIDATION_PROBLEM_DETAIL));
        }
    }

    /** An operation with nothing to send cannot fail validation, so it gets no documented 400. */
    private boolean rejectsInput(Operation operation) {
        return operation.getRequestBody() != null
                || (operation.getParameters() != null && !operation.getParameters().isEmpty());
    }

    private ApiResponse response(String description, String schemaName) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(PROBLEM_JSON,
                        new MediaType().schema(new Schema<>().$ref(SCHEMA_REF + schemaName))));
    }

    private Schema<?> problemDetail() {
        return new ObjectSchema()
                .description("RFC 7807 error body. Branch on 'code'; 'detail' is prose and may be "
                        + "reworded at any time. See docs/error-contract.md.")
                .addProperty("type", new StringSchema().format("uri")._default("about:blank")
                        .description("Omitted while it is about:blank for every error"))
                .addProperty("title", new StringSchema().example("Unauthorized")
                        .description("The HTTP reason phrase"))
                .addProperty("status", new IntegerSchema().example(401))
                .addProperty("detail", new StringSchema().example("The access token has expired")
                        .description("Human-readable; safe to show as a banner, never parse it"))
                .addProperty("instance", new StringSchema().example("/api/v1/cart")
                        .description("The request path"))
                .addProperty("code", new StringSchema().example("TOKEN_EXPIRED")
                        .description("Stable machine-readable symbol, e.g. TOKEN_EXPIRED, "
                                + "INSUFFICIENT_ROLE, VALIDATION_ERROR, MALFORMED_REQUEST"))
                .required(List.of("status", "title", "code"));
    }

    private Schema<?> validationProblemDetail() {
        Schema<?> errors = new ObjectSchema()
                .addProperty("errors", new MapSchema()
                        .additionalProperties(new ArraySchema().items(new StringSchema()))
                        .description("Field name to the messages that field broke. A field can "
                                + "break several constraints, so values are always arrays. Errors "
                                + "belonging to the payload as a whole use the key '_'."));

        return new Schema<>()
                .description("A ProblemDetail carrying the per-field breakdown of a rejected payload")
                .addAllOfItem(new Schema<>().$ref(SCHEMA_REF + PROBLEM_DETAIL))
                .addAllOfItem(errors);
    }
}
