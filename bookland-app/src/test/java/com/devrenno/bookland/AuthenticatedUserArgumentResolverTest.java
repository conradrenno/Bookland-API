package com.devrenno.bookland;

import com.devrenno.bookland.websupport.security.AuthenticatedUser;
import com.devrenno.bookland.websupport.security.AuthenticatedUserArgumentResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The resolver is the single translation point between the authentication mechanism and the user id
 * the domain stores, so what it does when it cannot translate is the whole point of it existing.
 *
 * <p>What it replaced returned {@code null}, copy-pasted into six controllers. A null customer id
 * does not fail: it flows into {@code findByCustomerId} and answers with an empty cart that belongs
 * to nobody, which is why the two ways of failing below are pinned as loudly as the happy path.
 *
 * <p>Lives in bookland-app, like {@code ProblemDetailErrorControllerTest} — bookland-web-support
 * carries no test scope of its own.
 */
class AuthenticatedUserArgumentResolverTest {

    private final AuthenticatedUserArgumentResolver resolver = new AuthenticatedUserArgumentResolver();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("claims an AuthenticatedUser parameter and nothing else")
    void supportsOnlyItsOwnType() throws Exception {
        assertThat(resolver.supportsParameter(parameterOf(0))).isTrue();
        assertThat(resolver.supportsParameter(parameterOf(1))).isFalse();
    }

    @Test
    @DisplayName("hands the handler the id the filter put in the details, plus the caller's email")
    void resolvesTheAuthenticatedCaller() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId, "customer@bookland.com");

        AuthenticatedUser caller = resolver.resolveArgument(parameterOf(0), null, null, null);

        assertThat(caller.id()).isEqualTo(userId);
        assertThat(caller.email()).isEqualTo("customer@bookland.com");
    }

    @Test
    @DisplayName("no authentication at all: asks for credentials rather than resolving to null")
    void rejectsAnEmptyContext() {
        assertThatThrownBy(() -> resolver.resolveArgument(parameterOf(0), null, null, null))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    /**
     * The case a plain {@code getAuthentication() != null} check would miss:
     * {@code AnonymousAuthenticationFilter} is left enabled, and its token reports
     * {@code isAuthenticated() == true}.
     */
    @Test
    @DisplayName("anonymous is not authenticated, however cheerfully the token reports otherwise")
    void rejectsAnonymous() {
        Authentication anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        assertThatThrownBy(() -> resolver.resolveArgument(parameterOf(0), null, null, null))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    /**
     * Authenticated but carrying no id is a wiring bug — the filter and this resolver having drifted
     * apart, which is exactly what a change of authentication mechanism causes. It must not be
     * mistaken for a client error, so it is not an {@code AuthenticationException}.
     */
    @Test
    @DisplayName("authenticated without an id in the details: fails as a server bug, not a 401")
    void rejectsAnAuthenticationWithoutAUserId() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "customer@bookland.com", null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        auth.setDetails("not-a-uuid");
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(() -> resolver.resolveArgument(parameterOf(0), null, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no user id");
    }

    private void authenticate(UUID userId, String email) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        auth.setDetails(userId);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private MethodParameter parameterOf(int index) throws NoSuchMethodException {
        Method handler = getClass().getDeclaredMethod("handler", AuthenticatedUser.class, UUID.class);
        return new MethodParameter(handler, index);
    }

    @SuppressWarnings("unused")
    private void handler(AuthenticatedUser caller, UUID bookId) {
    }
}
