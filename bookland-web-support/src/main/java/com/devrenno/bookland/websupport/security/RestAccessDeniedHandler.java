package com.devrenno.bookland.websupport.security;

import com.devrenno.bookland.websupport.ProblemDetailWriter;
import com.devrenno.bookland.websupport.ProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/**
 * Answers authenticated-but-not-allowed requests with 403 + problem+json. Reached only once
 * authentication succeeded — an anonymous request is routed to {@link RestAuthenticationEntryPoint}
 * and gets a 401 instead.
 */
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ProblemDetailWriter writer;

    public RestAccessDeniedHandler(ProblemDetailWriter writer) {
        this.writer = writer;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        AuthErrorCode error = AuthErrorCode.INSUFFICIENT_ROLE;

        response.setHeader(HttpHeaders.WWW_AUTHENTICATE,
                "Bearer error=\"insufficient_scope\", error_description=\"" + error.detail() + "\"");
        writer.write(request, response,
                ProblemDetails.of(error.status(), error.detail(), error.name()));
    }
}
