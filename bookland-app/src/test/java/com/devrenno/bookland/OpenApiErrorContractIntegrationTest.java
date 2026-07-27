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
            "/api/v1/users/{id},                           delete, 204",
            "/api/v1/admin/payments/order/{orderId}/refund, post,  204"
    })
    @DisplayName("handlers answering a non-200 status say so in the document")
    void successCodesMatchTheHandlers(String path, String method, String expected) throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['" + path + "']." + method + ".responses." + expected).exists())
                .andExpect(jsonPath("$.paths['" + path + "']." + method + ".responses.200").doesNotExist());
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
