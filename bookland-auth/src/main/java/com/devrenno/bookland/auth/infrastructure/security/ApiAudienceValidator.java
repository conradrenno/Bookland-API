package com.devrenno.bookland.auth.infrastructure.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Refuses a token that was not addressed to this API.
 *
 * <p>Without it, an {@code id_token} works as a Bearer credential. Two facts combine to make that
 * so: the default {@code JwtDecoder} validates signature, expiry and issuer but <em>no</em>
 * audience at all, and the generator gives both tokens the same {@code aud} — verified in the
 * bytecode of {@code JwtGenerator}, which builds the audience from {@code getClientId()} once,
 * with no branch on token type.
 *
 * <p>So this validator is only half a fix. The other half is in {@link BooklandTokenCustomizer},
 * which gives the access token an audience of its own; asking for {@code client_id} here would
 * accept both tokens and separate nothing. Neither half works alone, and the pair is pinned by the
 * test that presents an {@code id_token} to the API and expects a 401.
 */
public class ApiAudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final String audience;

    public ApiAudienceValidator(String audience) {
        this.audience = audience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (token.getAudience().contains(audience)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                OAuth2ErrorCodes.INVALID_TOKEN,
                "The token is not addressed to this API",
                null));
    }
}
