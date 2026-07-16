package com.devrenno.bookland.auth.domain.valueobject;

import com.devrenno.bookland.auth.domain.entity.RefreshToken;

/**
 * Domain result of an authentication use case: the freshly issued access token (a {@link Token}
 * value object) paired with its persisted {@link RefreshToken}. Shaped for delivery by the Presenter.
 */
public record AuthTokens(Token accessToken, RefreshToken refreshToken) {}
