package com.devrenno.bookland;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The published document has to describe the error bodies, or a client generating types off it ends
 * up hand-writing the one type it is guaranteed to receive.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class OpenApiErrorContractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("the document declares both error schemas, with the code member")
    void errorSchemasArePublished() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.ProblemDetail.properties.code").exists())
                .andExpect(jsonPath("$.components.schemas.ProblemDetail.properties.detail").exists())
                .andExpect(jsonPath("$.components.schemas.ValidationProblemDetail.allOf").isArray());
    }

    @Test
    @DisplayName("an operation taking a body documents both the default error and a 400")
    void operationsDocumentTheirErrors() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/auth/register'].post.responses.default").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/register'].post.responses.400"
                        + ".content['application/problem+json'].schema.$ref")
                        .value("#/components/schemas/ValidationProblemDetail"));
    }

    /**
     * Springdoc infers 200 from the return type and only learns otherwise from {@code @ResponseStatus}
     * — it cannot see through {@code ResponseEntity.status(...)}. Every handler answering something
     * else is listed here, so dropping the annotation shows up as a failing test rather than as a
     * client generating the wrong success type.
     */
    @ParameterizedTest(name = "{1} {0} is documented as {2}")
    @CsvSource({
            "/api/v1/auth/register,                        post,   201",
            "/api/v1/auth/logout,                          post,   204",
            "/api/v1/books,                                post,   201",
            "/api/v1/books/{bookId},                       delete, 204",
            "/api/v1/books/{bookId}/reviews,               post,   201",
            "/api/v1/books/{bookId}/reviews/{reviewId},    delete, 204",
            "/api/v1/users/{id},                           delete, 204"
    })
    @DisplayName("handlers answering a non-200 status say so in the document")
    void successCodesMatchTheHandlers(String path, String method, String expected) throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['" + path + "']." + method + ".responses." + expected).exists())
                .andExpect(jsonPath("$.paths['" + path + "']." + method + ".responses.200").doesNotExist());
    }

    /**
     * {@code AuthenticatedUser} is supplied from the SecurityContext by an argument resolver, but to
     * springdoc an unannotated POJO parameter is a set of query parameters to expand. Without the
     * ignore registered in {@code OpenApiConfig} the document would offer `id` and `email` as query
     * params on every handler taking a caller — an invitation to spoof another customer that the
     * server would simply discard, which is the worst way for a document to be wrong.
     */
    @Test
    @DisplayName("the resolved caller is not published as a query parameter")
    void theAuthenticatedCallerIsNotAParameter() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/cart'].get.parameters").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/wishlist'].get.parameters").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/orders'].get.parameters[*].name")
                        .value(org.hamcrest.Matchers.containsInAnyOrder("page", "size")));
    }

    /**
     * A refund is one half of a cancellation. Exposed on its own it left the order CONFIRMED and the
     * stock never returned — the mirror of the admin-cancellation bug, and the last caller that wrote
     * the effect outside the decision. The whole operation is
     * {@code PATCH /admin/orders/{orderId}/status} → CANCELLED, which compensates through
     * {@code OrderCancellation}. Re-publishing the route means deleting this test, which is the point.
     */
    @Test
    @DisplayName("no route issues a refund on its own")
    void refundIsNotAnEndpointOfItsOwn() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/admin/payments/order/{orderId}/refund']")
                        .doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/admin/orders/{orderId}/status'].patch")
                        .exists());
    }

    @Test
    @DisplayName("the bearer scheme is declared, so the UI can authorize")
    void bearerSchemeIsDeclared() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));
    }
}
