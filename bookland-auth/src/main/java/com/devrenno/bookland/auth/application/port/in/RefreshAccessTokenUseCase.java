package com.devrenno.bookland.auth.application.port.in;

import com.devrenno.bookland.auth.domain.valueobject.AuthTokens;

public interface RefreshAccessTokenUseCase {
    AuthTokens execute(String refreshTokenValue);
}
