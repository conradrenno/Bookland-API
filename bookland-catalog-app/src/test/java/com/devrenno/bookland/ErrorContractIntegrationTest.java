package com.devrenno.bookland;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The error contract ({@code docs/error-contract.md}) as the catalog service answers it — the same
 * contract the monolith and the identity service answer, now that the catalog is a process of its
 * own: business codes, /error outside the authentication wall, and the OpenAPI document describing
 * both error shapes and the real success codes.
 */
@CatalogIntegrationTest
class ErrorContractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("404 carries BOOK_NOT_FOUND, with a detail and no errors map")
    void bookNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/books/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOK_NOT_FOUND"))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    /** While /error required authentication, every 500 reached the client as a fake 401. */
    @Test
    @DisplayName("/error is reachable without a token, and answers INTERNAL_ERROR")
    void errorPathIsNotBehindAuthentication() throws Exception {
        mockMvc.perform(get("/error"))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @Test
    @DisplayName("an unmapped protected path stays 401, and does not reveal that it is unmapped")
    void unmappedProtectedPathDoesNotLeakItsAbsence() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_MISSING"));
    }

    @Test
    @DisplayName("the document declares both error schemas, and a body-taking operation its 400")
    void errorSchemasArePublished() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Bookland Catalog API"))
                .andExpect(jsonPath("$.components.schemas.ProblemDetail.properties.code").exists())
                .andExpect(jsonPath("$.components.schemas.ValidationProblemDetail.allOf").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/books'].post.responses.400"
                        + ".content['application/problem+json'].schema.$ref")
                        .value("#/components/schemas/ValidationProblemDetail"));
    }

    /** springdoc infers 200 from the return type; only @ResponseStatus tells it otherwise. */
    @ParameterizedTest(name = "{1} {0} is documented as {2}")
    @CsvSource({
            "/api/v1/books,          post,   201",
            "/api/v1/books/{bookId}, delete, 204"
    })
    @DisplayName("handlers answering a non-200 status say so in the document")
    void successCodesMatchTheHandlers(String path, String method, String expected) throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(jsonPath("$.paths['" + path + "']." + method + ".responses." + expected).exists())
                .andExpect(jsonPath("$.paths['" + path + "']." + method + ".responses.200").doesNotExist());
    }

    @Test
    @DisplayName("the login URLs name the identity service, and the UI runs PKCE with Basic client auth")
    void securitySchemesAndSwaggerUi() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(jsonPath("$.components.securitySchemes.oauth2.flows.authorizationCode.tokenUrl")
                        .value("http://127.0.0.1:9000/oauth2/token"));
        mockMvc.perform(get("/swagger-ui/swagger-initializer.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"usePkceWithAuthorizationCodeGrant\":true")))
                .andExpect(content().string(containsString("\"useBasicAuthenticationWithAccessCodeGrant\":true")));
    }
}
