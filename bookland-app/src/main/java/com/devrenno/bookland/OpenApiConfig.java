package com.devrenno.bookland;

import com.devrenno.bookland.websupport.openapi.ErrorResponsesCustomizer;
import com.devrenno.bookland.websupport.security.AuthenticatedUser;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String OAUTH2 = "oauth2";
    private static final String BEARER_AUTH = "bearerAuth";

    @Value("${bookland.oauth2.issuer}")
    private String issuer;

    static {
        // AuthenticatedUser comes from the SecurityContext, never from the request. springdoc has
        // no way to know that: to it an unannotated POJO parameter is a set of query parameters,
        // so without this every handler taking a caller would publish bogus `id` and `email` query
        // params and every generated client would offer to spoof them.
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(AuthenticatedUser.class);
    }

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
                .components(new Components()
                        .addSecuritySchemes(OAUTH2, authorizationCodeScheme())
                        .addSecuritySchemes(BEARER_AUTH, bearerScheme()))
                // Two schemes, listed as separate requirements so either satisfies an operation.
                // Endpoints that are public simply ignore the header — see ApiSecurityConfig for the
                // actual rules, which are not derivable from the handlers and are deliberately not
                // duplicated here.
                .addSecurityItem(new SecurityRequirement().addList(OAUTH2))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    /**
     * Lets the Swagger UI run the real login: it redirects to the Authorization Server, comes back
     * with a code and exchanges it, all without anyone pasting a token.
     *
     * <p>The URLs are built from the configured issuer rather than hardcoded, because the issuer is
     * what the Authorization Server publishes and a document pointing somewhere else would send the
     * UI to a host that never issued anything.
     *
     * <p>Note the UI must be configured with PKCE, since the registered client requires it — that is
     * {@code springdoc.swagger-ui.use-pkce-with-authorization-code-grant} in application.yml.
     */
    private SecurityScheme authorizationCodeScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .description("Authorization code + PKCE against this application's own Authorization Server.")
                .flows(new OAuthFlows().authorizationCode(new OAuthFlow()
                        .authorizationUrl(issuer + "/oauth2/authorize")
                        .tokenUrl(issuer + "/oauth2/token")
                        .scopes(new Scopes()
                                .addString("openid", "Issue an id_token identifying the user")
                                .addString("profile", "Basic profile claims")
                                .addString("email", "The user's e-mail claim"))));
    }

    /**
     * Kept alongside the flow above for the case the flow cannot be used — a token obtained out of
     * band, or a UI running on an origin the client has no redirect URI for.
     */
    private SecurityScheme bearerScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("An access token obtained from POST /oauth2/token.");
    }

    @Bean
    public OpenApiCustomizer errorResponsesCustomizer() {
        return new ErrorResponsesCustomizer();
    }
}
