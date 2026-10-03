package com.devrenno.bookland.auth.infrastructure.security;

import com.devrenno.bookland.auth.application.dto.AuthUserDto;
import com.devrenno.bookland.auth.application.port.out.UserLookupPort;
import com.devrenno.bookland.user.domain.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The customizer is where the two claims the rest of Bookland depends on are written, and both of
 * them fail in ways no compiler catches. These tests are the trap.
 */
class BooklandTokenCustomizerTest {

    private static final String API_AUDIENCE = "bookland-api";
    private static final String CLIENT_ID = "bookland-web";

    private final UserLookupPort userLookupPort = mock(UserLookupPort.class);
    private final BooklandTokenCustomizer customizer = new BooklandTokenCustomizer(API_AUDIENCE, userLookupPort);

    private final UUID userId = UUID.randomUUID();

    /**
     * Identity reaches the token as a single string propagated from {@code getUsername()}, so the
     * default {@code sub} is whatever was declared as the login identifier — the e-mail. Every
     * business column in the schema stores the user id instead, and after the filter's removal
     * {@code sub} is the only channel identity travels through.
     */
    @Test
    @DisplayName("the access token's sub is the user id, not the e-mail")
    void accessTokenSubjectIsTheUserId() {
        JwtEncodingContext context = contextFor(OAuth2TokenType.ACCESS_TOKEN);

        customizer.customize(context);

        assertThat(subjectOf(context)).isEqualTo(userId.toString());
        assertThat(subjectOf(context)).isNotEqualTo("admin@bookland.com");
    }

    /**
     * The test that catches an {@code if (tokenType == ACCESS_TOKEN)} wrapped around the subject.
     * Such a guard looks harmless and fixes half the system: the API reads the access token and a
     * BFF reads the id_token, so the two would come to know the same person by two identities — one
     * a UUID, the other an e-mail. Nothing fails until a BFF exists.
     */
    @Test
    @DisplayName("the id_token's sub is the same user id — the subject is never filtered by token type")
    void idTokenSubjectIsTheSameUserId() {
        JwtEncodingContext context = contextFor(new OAuth2TokenType("id_token"));

        customizer.customize(context);

        assertThat(subjectOf(context)).isEqualTo(userId.toString());
    }

    /**
     * The claim that must differ, and the reason an audience validator alone separates nothing: by
     * default the generator gives both tokens {@code aud = client_id}.
     */
    @Test
    @DisplayName("only the access token is re-addressed to the API")
    void onlyTheAccessTokenCarriesTheApiAudience() {
        JwtEncodingContext accessToken = contextFor(OAuth2TokenType.ACCESS_TOKEN);
        JwtEncodingContext idToken = contextFor(new OAuth2TokenType("id_token"));

        customizer.customize(accessToken);
        customizer.customize(idToken);

        assertThat(audienceOf(accessToken)).containsExactly(API_AUDIENCE);
        assertThat(audienceOf(idToken)).containsExactly(CLIENT_ID);
    }

    /**
     * {@code email} stops being implicit in {@code sub} and has to be carried explicitly — it is
     * what {@code AuthenticatedUserArgumentResolver} reads, and {@code role} is what becomes the
     * authority every {@code hasRole} rule asks about.
     */
    @Test
    @DisplayName("email and role travel as claims of their own")
    void emailAndRoleAreExplicitClaims() {
        JwtEncodingContext context = contextFor(OAuth2TokenType.ACCESS_TOKEN);

        customizer.customize(context);

        assertThat(claim(context, "email")).isEqualTo("admin@bookland.com");
        assertThat(claim(context, "role")).isEqualTo("ADMIN");
    }

    /**
     * A principal the customizer does not recognise must leave the claims alone rather than write a
     * subject it invented. The loud failure then happens downstream, in the resolver, which refuses
     * a subject that is not a user id.
     */
    @Test
    @DisplayName("an unrecognised principal leaves the subject untouched")
    void unknownPrincipalIsLeftAlone() {
        JwtEncodingContext context = JwtEncodingContext
                .with(JwsHeader.with(SignatureAlgorithm.RS256), baseClaims())
                .principal(new UsernamePasswordAuthenticationToken("someone", null, List.of()))
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .build();

        customizer.customize(context);

        assertThat(subjectOf(context)).isEqualTo("admin@bookland.com");
    }

    /**
     * The principal on a refresh is the one stored at login. The framework issues the new token from
     * it without asking any user store, so without this lookup a deleted account keeps refreshing
     * for the refresh token's whole lifetime.
     */
    @Test
    @DisplayName("a refresh for an account that no longer exists is invalid_grant")
    void refreshForADeletedAccountIsRefused() {
        when(userLookupPort.findActiveById(userId)).thenReturn(Optional.empty());
        JwtEncodingContext context = contextFor(OAuth2TokenType.ACCESS_TOKEN, AuthorizationGrantType.REFRESH_TOKEN);

        assertThatThrownBy(() -> customizer.customize(context))
                .isInstanceOfSatisfying(OAuth2AuthenticationException.class, e ->
                        assertThat(e.getError().getErrorCode()).isEqualTo(OAuth2ErrorCodes.INVALID_GRANT));
    }

    @Test
    @DisplayName("a refresh writes the role the account has now, not the one stored at login")
    void refreshWritesTheCurrentRole() {
        when(userLookupPort.findActiveById(userId)).thenReturn(Optional.of(
                new AuthUserDto(userId, "admin@bookland.com", "hashed", UserRole.CUSTOMER, true)));
        JwtEncodingContext context = contextFor(OAuth2TokenType.ACCESS_TOKEN, AuthorizationGrantType.REFRESH_TOKEN);

        customizer.customize(context);

        assertThat(claim(context, "role")).isEqualTo("CUSTOMER");
    }

    @Test
    @DisplayName("the code exchange trusts the principal it just authenticated — no lookup")
    void codeExchangeDoesNotLookTheAccountUp() {
        customizer.customize(contextFor(OAuth2TokenType.ACCESS_TOKEN, AuthorizationGrantType.AUTHORIZATION_CODE));

        verifyNoInteractions(userLookupPort);
    }

    private JwtEncodingContext contextFor(OAuth2TokenType tokenType) {
        return contextFor(tokenType, AuthorizationGrantType.AUTHORIZATION_CODE);
    }

    private JwtEncodingContext contextFor(OAuth2TokenType tokenType, AuthorizationGrantType grantType) {
        BooklandUserDetails principal = new BooklandUserDetails(
                userId, "admin@bookland.com", "hashed", UserRole.ADMIN, true);

        return JwtEncodingContext
                .with(JwsHeader.with(SignatureAlgorithm.RS256), baseClaims())
                .principal(new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities()))
                .tokenType(tokenType)
                .authorizationGrantType(grantType)
                .build();
    }

    /** What the generator produces before the customizer runs: subject and audience both default. */
    private JwtClaimsSet.Builder baseClaims() {
        return JwtClaimsSet.builder()
                .issuer("http://localhost:8080")
                .subject("admin@bookland.com")
                .audience(Collections.singletonList(CLIENT_ID))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900));
    }

    private String subjectOf(JwtEncodingContext context) {
        return context.getClaims().build().getSubject();
    }

    private List<String> audienceOf(JwtEncodingContext context) {
        return context.getClaims().build().getAudience();
    }

    private Object claim(JwtEncodingContext context, String name) {
        return context.getClaims().build().getClaim(name);
    }
}
