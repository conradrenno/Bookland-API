package com.devrenno.bookland.websupport.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * Records <em>why</em> a token was rejected, then lets the shared entry point render it.
 *
 * <p>This is the seam the removal of {@code JwtAuthenticationFilter} left behind. That filter caught
 * its own exceptions and wrote {@link AuthErrorCode#REQUEST_ATTRIBUTE} before letting the request
 * carry on; the resource server's filter does neither — it throws an
 * {@code AuthenticationException} and hands it to the entry point. By then the only trace of the
 * cause is the exception chain.
 *
 * <p>It wraps rather than replaces {@code RestAuthenticationEntryPoint} on purpose: the shape of the
 * 401 body, the {@code WWW-Authenticate} challenge and the problem+json writing stay in
 * bookland-web-support, shared with everything else. What is OAuth2-specific — how to read a
 * {@code JwtValidationException} — is this class. Both halves live in this module because every
 * service that validates tokens needs both, and none of them should need bookland-auth to do it.
 *
 * <p>Setting no attribute is meaningful: {@code AuthErrorCode.fromRequest} defaults to
 * {@code TOKEN_MISSING}, which is the right answer when the request never carried a credential and
 * the denial came from the authorization filter rather than from decoding.
 */
public class BearerTokenErrorClassifier implements AuthenticationEntryPoint {

    private final AuthenticationEntryPoint delegate;

    public BearerTokenErrorClassifier(AuthenticationEntryPoint delegate) {
        this.delegate = delegate;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        AuthErrorCode classified = classify(authException);
        if (classified != null) {
            request.setAttribute(AuthErrorCode.REQUEST_ATTRIBUTE, classified);
        }
        delegate.commence(request, response, authException);
    }

    /**
     * @return the reason, or {@code null} when no credential was presented — leaving the default
     *         {@code TOKEN_MISSING} in place rather than asserting something about a token that
     *         never existed.
     */
    private AuthErrorCode classify(AuthenticationException exception) {
        JwtValidationException validation = findValidationFailure(exception);

        if (validation == null) {
            // A token was presented and could not even be parsed or verified — malformed, badly
            // signed, wrong key. Refreshing will not help, which is what TOKEN_INVALID tells the
            // client. A denial with no token at all reaches here as an authorization failure
            // carrying no JWT cause either, so it is separated by the check below.
            return carriesToken(exception) ? AuthErrorCode.TOKEN_INVALID : null;
        }

        boolean expired = validation.getErrors().stream()
                .map(OAuth2Error::getErrorCode)
                .anyMatch(AccessTokenExpiryValidator.ERROR_CODE::equals);

        return expired ? AuthErrorCode.TOKEN_EXPIRED : AuthErrorCode.TOKEN_INVALID;
    }

    private JwtValidationException findValidationFailure(Throwable throwable) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof JwtValidationException validation) {
                return validation;
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        return null;
    }

    /**
     * The resource server rejects a presented token by throwing {@code InvalidBearerTokenException},
     * which is an {@link OAuth2AuthenticationException}. A request carrying no credential is denied
     * later and elsewhere — by the authorization filter, as an
     * {@code InsufficientAuthenticationException} — and that plain type is what separates the two.
     */
    private boolean carriesToken(AuthenticationException exception) {
        return exception instanceof OAuth2AuthenticationException;
    }
}
