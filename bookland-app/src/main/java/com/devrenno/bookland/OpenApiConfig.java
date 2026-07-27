package com.devrenno.bookland;

import com.devrenno.bookland.websupport.openapi.ErrorResponsesCustomizer;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI booklandOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bookland API")
                        .version("v1")
                        .description("""
                                Online bookstore API.

                                Errors follow RFC 7807 (application/problem+json) with a
                                machine-readable `code`; see docs/error-contract.md. In particular
                                401 and 403 are distinct: 401 means the token is missing or no
                                longer good (refresh, or send the user to login), 403 means the
                                token is fine but the role is not — retrying is pointless."""))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Access token from POST /api/v1/auth/login")))
                // Applied document-wide so the Swagger UI has one Authorize button. Endpoints that
                // are public simply ignore the header — see SecurityConfig for the actual rules,
                // which are not derivable from the handlers and are deliberately not duplicated
                // here.
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    @Bean
    public OpenApiCustomizer errorResponsesCustomizer() {
        return new ErrorResponsesCustomizer();
    }
}
