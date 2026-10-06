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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

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
    @DisplayName("hands the handler the id from sub, plus the email and name from their own claims")
    void resolvesTheAuthenticatedCaller() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId, "customer@bookland.com");

        AuthenticatedUser caller = resolver.resolveArgument(parameterOf(0), null, null, null);

        assertThat(caller.id()).isEqualTo(userId);
        assertThat(caller.email()).isEqualTo("customer@bookland.com");
        assertThat(caller.name()).isEqualTo("Ana Souza");
    }

    /** A token issued before the claim existed still identifies its caller; only the name is missing. */
    @Test
    @DisplayName("a token without a name claim still resolves, with a null name")
    void resolvesWithoutANameClaim() throws Exception {
        UUID userId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                Jwt.withTokenValue("token").header("alg", "RS256")
                        .subject(userId.toString()).claim("email", "customer@bookland.com").build(),
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

        AuthenticatedUser caller = resolver.resolveArgument(parameterOf(0), null, null, null);

        assertThat(caller.id()).isEqualTo(userId);
        assertThat(caller.name()).isNull();
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
     * Authenticated but not by a token is a wiring bug — the filter and this resolver having drifted
     * apart, which is exactly what a change of authentication mechanism causes. It must not be
     * mistaken for a client error, so it is not an {@code AuthenticationException}.
     */
    @Test
    @DisplayName("authenticated by something other than a JWT: fails as a server bug, not a 401")
    void rejectsAnAuthenticationWithoutAToken() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "customer@bookland.com", null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(() -> resolver.resolveArgument(parameterOf(0), null, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not backed by a JWT");
    }

    /**
     * The trap section 3.3 of the plan is about: leave the token customizer out and {@code sub}
     * falls back to the e-mail. It has to be loud, because that value flows on into
     * {@code customer_id}.
     */
    @Test
    @DisplayName("a sub that is not a user id fails rather than being passed on as a caller")
    void rejectsASubjectThatIsNotAUserId() {
        SecurityContextHolder.getContext().setAuthentication(
                tokenFor("customer@bookland.com", "customer@bookland.com"));

        assertThatThrownBy(() -> resolver.resolveArgument(parameterOf(0), null, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not a Bookland user id");
    }

    /**
     * The silent one: {@code getName()} returns the subject, so reading the e-mail from it would
     * fill the field with a UUID. Nothing breaks — the type is {@code String} either way — and every
     * log line naming the caller quietly starts showing an opaque identifier.
     */
    @Test
    @DisplayName("the email comes from the email claim, not from the subject")
    void emailIsReadFromItsOwnClaim() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId, "customer@bookland.com");

        AuthenticatedUser caller = resolver.resolveArgument(parameterOf(0), null, null, null);

        assertThat(caller.email()).doesNotContain(userId.toString());
    }

    private void authenticate(UUID userId, String email) {
        SecurityContextHolder.getContext().setAuthentication(tokenFor(userId.toString(), email));
    }

    private JwtAuthenticationToken tokenFor(String subject, String email) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(subject)
                .claim("email", email)
                .claim("name", "Ana Souza")
                .build();
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    private MethodParameter parameterOf(int index) throws NoSuchMethodException {
        Method handler = getClass().getDeclaredMethod("handler", AuthenticatedUser.class, UUID.class);
        return new MethodParameter(handler, index);
    }

    @SuppressWarnings("unused")
    private void handler(AuthenticatedUser caller, UUID bookId) {
    }
}
