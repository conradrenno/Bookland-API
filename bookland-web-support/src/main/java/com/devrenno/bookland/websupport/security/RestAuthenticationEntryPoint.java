package com.devrenno.bookland.websupport.security;

import com.devrenno.bookland.websupport.ProblemDetailWriter;
import com.devrenno.bookland.websupport.ProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * Answers unauthenticated requests with 401 + problem+json.
 *
 * <p>Replaces Spring Security's default for a chain with no interactive authentication mechanism
 * ({@code Http403ForbiddenEntryPoint}), which answers 403 with an empty body and leaves a client
 * unable to tell an expired token from a missing role.
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ProblemDetailWriter writer;

    public RestAuthenticationEntryPoint(ProblemDetailWriter writer) {
        this.writer = writer;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        AuthErrorCode error = AuthErrorCode.fromRequest(request);

        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, challenge(error));
        writer.write(request, response,
                ProblemDetails.of(error.status(), error.detail(), error.name()));
    }

    /**
     * RFC 6750 §3: the {@code error} parameter is only sent when a credential was actually
     * presented — a bare {@code Bearer} challenge is the correct answer to a request with none.
     */
    private String challenge(AuthErrorCode error) {
        if (error == AuthErrorCode.TOKEN_MISSING) {
            return "Bearer";
        }
        return "Bearer error=\"invalid_token\", error_description=\"" + error.detail() + "\"";
    }
}
