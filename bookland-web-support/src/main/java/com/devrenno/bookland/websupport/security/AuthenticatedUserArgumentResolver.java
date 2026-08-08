package com.devrenno.bookland.websupport.security;

import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

/**
 * Supplies {@link AuthenticatedUser} to any handler that declares one, translating whatever the
 * authenticating filter put in the {@code SecurityContext} into the identity the domain speaks.
 *
 * <p>This is the single point of contact between the authentication mechanism and the web layer.
 * The mapping it performs is not something a security framework can do for us: a filter establishes
 * <em>a</em> principal, but which row of {@code users} that principal is remains an application
 * decision. Today the answer is trivial — the filter itself put the id in
 * {@code Authentication.getDetails()}. Under an OAuth2 resource server it becomes a claim read, and
 * under an external identity provider a lookup from the issuer's {@code sub}. All three are edits
 * to this class, not to fifteen handler signatures.
 *
 * <p><strong>It never returns null.</strong> The method it replaced did, which meant a route
 * mistakenly left public, or a filter that stopped populating the details, produced a null customer
 * id that flowed into a query and quietly returned somebody else's empty cart instead of failing.
 * The two ways this can go wrong are now distinct and both loud:
 *
 * <ul>
 *   <li><em>Nobody is authenticated</em> — an {@link AuthenticationCredentialsNotFoundException}
 *       propagates out of the DispatcherServlet to {@code ExceptionTranslationFilter}, which is
 *       still on the stack, and comes back as the contract's 401 {@code TOKEN_MISSING}. That is the
 *       honest answer: the handler needs a caller and the request has none.</li>
 *   <li><em>Somebody is authenticated but carries no id</em> — a wiring bug, not a client error, so
 *       it surfaces as a 500 {@code INTERNAL_ERROR} through {@code ProblemDetailErrorController}.
 *       The message names the drift for the log; the client never sees it.</li>
 * </ul>
 */
public class AuthenticatedUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return AuthenticatedUser.class.equals(parameter.getParameterType());
    }

    @Override
    public AuthenticatedUser resolveArgument(MethodParameter parameter,
                                             ModelAndViewContainer mavContainer,
                                             NativeWebRequest webRequest,
                                             WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!isAuthenticated(authentication)) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Handler " + parameter.getExecutable() + " requires an authenticated caller "
                            + "and the request has none — check the SecurityConfig rule for this route");
        }

        if (!(authentication.getDetails() instanceof UUID userId)) {
            throw new IllegalStateException(
                    "The authentication in the SecurityContext carries no user id in its details ("
                            + authentication.getClass().getName() + "). The authenticating filter "
                            + "and " + getClass().getSimpleName() + " have drifted apart.");
        }

        return new AuthenticatedUser(userId, authentication.getName());
    }

    /**
     * The anonymous check matters: {@code AnonymousAuthenticationFilter} is left enabled, so an
     * unauthenticated request arrives here holding an {@code AnonymousAuthenticationToken} that
     * reports {@code isAuthenticated() == true}.
     */
    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
