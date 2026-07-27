package com.devrenno.bookland;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

    @Test
    @DisplayName("the bearer scheme is declared, so the UI can authorize")
    void bearerSchemeIsDeclared() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));
    }
}
