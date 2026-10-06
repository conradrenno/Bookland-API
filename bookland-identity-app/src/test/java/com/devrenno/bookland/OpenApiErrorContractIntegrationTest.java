package com.devrenno.bookland;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The identity service publishes the same contract as the monolith — {@code OpenApiConfig} and
 * {@code ErrorResponsesCustomizer} come from web-support — for its own routes.
 */
@IdentityIntegrationTest
class OpenApiErrorContractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("the document is this service's, with both error schemas")
    void documentIsPublished() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Bookland Identity API"))
                .andExpect(jsonPath("$.components.schemas.ProblemDetail.properties.code").exists())
                .andExpect(jsonPath("$.components.schemas.ValidationProblemDetail.allOf").isArray());
    }

    @Test
    @DisplayName("registering documents both the default error and a 400")
    void registerDocumentsItsErrors() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/auth/register'].post.responses.default").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/register'].post.responses.400"
                        + ".content['application/problem+json'].schema.$ref")
                        .value("#/components/schemas/ValidationProblemDetail"));
    }

    /** See the monolith's twin: springdoc cannot see through ResponseEntity.status(...). */
    @ParameterizedTest(name = "{1} {0} is documented as {2}")
    @CsvSource({
            "/api/v1/auth/register, post,   201",
            "/api/v1/users/{id},    delete, 204"
    })
    @DisplayName("handlers answering a non-200 status say so in the document")
    void successCodesMatchTheHandlers(String path, String method, String expected) throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['" + path + "']." + method + ".responses." + expected).exists())
                .andExpect(jsonPath("$.paths['" + path + "']." + method + ".responses.200").doesNotExist());
    }

    /** The login runs against this process, which is the Authorization Server. */
    @Test
    @DisplayName("the login URLs name this server")
    void loginUrlsNameThisServer() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.components.securitySchemes.oauth2.flows.authorizationCode.authorizationUrl")
                        .value("http://127.0.0.1:9000/oauth2/authorize"))
                .andExpect(jsonPath(
                        "$.components.securitySchemes.oauth2.flows.authorizationCode.tokenUrl")
                        .value("http://127.0.0.1:9000/oauth2/token"));
    }

    @Test
    @DisplayName("the Swagger UI runs the login the registered client accepts")
    void swaggerUiUsesPkceAndBasicClientAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui/swagger-initializer.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"usePkceWithAuthorizationCodeGrant\":true")))
                .andExpect(content().string(containsString("\"useBasicAuthenticationWithAccessCodeGrant\":true")));
    }
}
