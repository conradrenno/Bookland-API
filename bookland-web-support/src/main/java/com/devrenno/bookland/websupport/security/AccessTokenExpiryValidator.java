package com.devrenno.bookland.websupport.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Duration;
import java.time.Instant;

/**
 * Fails an expired token under an error code of our own, so that the entry point can tell
 * {@code TOKEN_EXPIRED} from {@code TOKEN_INVALID} without reading English prose.
 *
 * <p>That distinction is contract, not cosmetics: 401 {@code TOKEN_EXPIRED} tells a client to
 * refresh and retry, while {@code TOKEN_INVALID} tells it refreshing is pointless. It used to come
 * from two {@code catch} blocks in {@code JwtAuthenticationFilter}; with the resource server doing
 * the decoding, the only thing that survives to the entry point is the {@link OAuth2Error} list
 * inside {@code JwtValidationException}.
 *
 * <p>The framework's own {@code JwtTimestampValidator} reports expiry as {@code invalid_token} with
 * the description "Jwt expired at …" — the same code it uses for everything else, leaving the
 * message as the only discriminator. Matching on a human-readable string that no test of theirs
 * pins would break silently on an upgrade. Hence this validator, which runs alongside theirs
 * (they keep {@code nbf}, and their check of {@code exp} stays as the authoritative one) purely to
 * add a stable, machine-readable code.
 */
public class AccessTokenExpiryValidator implements OAuth2TokenValidator<Jwt> {

    /** Matched by {@link BearerTokenErrorClassifier}. Not part of the HTTP contract. */
    public static final String ERROR_CODE = "bookland_token_expired";

    /** The same default {@code JwtTimestampValidator} uses, so the two cannot disagree. */
    private static final Duration CLOCK_SKEW = Duration.ofSeconds(60);

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        Instant expiresAt = token.getExpiresAt();

        if (expiresAt != null && Instant.now().minus(CLOCK_SKEW).isAfter(expiresAt)) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error(ERROR_CODE, "The access token has expired", null));
        }
        return OAuth2TokenValidatorResult.success();
    }
}
