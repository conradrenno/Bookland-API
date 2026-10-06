package com.devrenno.bookland.auth.infrastructure.security;

import com.devrenno.bookland.auth.application.dto.AuthUserDto;
import com.devrenno.bookland.auth.application.port.out.UserLookupPort;
import com.devrenno.bookland.user.domain.entity.UserRole;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.Collections;

/**
 * Writes the claims the rest of Bookland reads. The interface the Authorization Server publishes
 * for precisely this — not a workaround, not a bean override.
 *
 * <p><strong>{@code sub} is set for both tokens, deliberately without a token-type check.</strong>
 * Wrapping it in {@code if (ACCESS_TOKEN)} fixes half the system: the API reads the access token and
 * a BFF reads the id_token, so the two would come to know the same person by two different
 * identities — the API by UUID, the BFF by e-mail. That divergence surfaces only once a BFF exists,
 * which is why the test suite pins the id_token's {@code sub} as well as the access token's.
 *
 * <p><strong>{@code aud} is the one claim that must differ.</strong> The default generator gives
 * both tokens {@code aud = client_id}, which means an audience validator asking for the client id
 * would accept either, and an id_token would pass as a Bearer at the API. The access token gets the
 * API's own audience here; the id_token keeps {@code client_id}, which is correct — it really is
 * addressed to the client. The other half of this is the validator on the resource server; neither
 * half works alone.
 *
 * <p>{@code email} becomes an explicit claim because it stops being implicit in {@code sub}. It is
 * the standard OIDC claim for it, and it is where {@code AuthenticatedUserArgumentResolver} will
 * read the caller's e-mail once {@code getName()} starts returning a UUID.
 *
 * <p>{@code name} is the standard OIDC claim for the display name, and it is how the name reaches
 * the services that keep one — reviews stores it on the review — without any of them calling the
 * user module. A refresh writes the current name, like the current role.
 *
 * <p><strong>A refresh re-reads the account.</strong> The principal handed to a refresh is the one
 * stored at login, up to seven days old, and the framework never consults a user store on that
 * path. Trusting it would keep issuing tokens to a deleted account, and with the role it had at
 * login. So on a refresh the account is looked up again by id: gone or deactivated is
 * {@code invalid_grant} — the standard signal for "log in again" — and otherwise the current role
 * and name are written, not the stored ones.
 */
public class BooklandTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    private final String apiAudience;
    private final UserLookupPort userLookupPort;

    public BooklandTokenCustomizer(String apiAudience, UserLookupPort userLookupPort) {
        this.apiAudience = apiAudience;
        this.userLookupPort = userLookupPort;
    }

    @Override
    public void customize(JwtEncodingContext context) {
        if (context.getPrincipal().getPrincipal() instanceof BooklandUserDetails user) {
            UserRole role = user.getRole();
            String name = user.getName();
            if (AuthorizationGrantType.REFRESH_TOKEN.equals(context.getAuthorizationGrantType())) {
                AuthUserDto current = userLookupPort.findActiveById(user.getUserId())
                        .orElseThrow(() -> new OAuth2AuthenticationException(new OAuth2Error(
                                OAuth2ErrorCodes.INVALID_GRANT, "The account no longer exists", null)));
                role = current.role();
                name = current.name();
            }
            context.getClaims()
                    .subject(user.getUserId().toString())
                    .claim("email", user.getUsername())
                    .claim("role", role.name());
            // The builder refuses a null claim value. Null only for a principal stored before the
            // name was carried, and only until that session's next refresh.
            if (name != null) {
                context.getClaims().claim("name", name);
            }
        }

        if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
            // Collections.singletonList, not List.of, and the difference is not cosmetic. The claim
            // map is persisted with polymorphic typing behind an allowlist, and the collection's
            // concrete class is written into it. Matching what the generator itself produces keeps
            // this token indistinguishable from a default one; List.of yields
            // ImmutableCollections$List12, which nothing allowlists, and the authorization then
            // fails to deserialise at /userinfo and on every refresh.
            context.getClaims().audience(Collections.singletonList(apiAudience));
        }
    }
}
