package com.devrenno.bookland.auth.application.port.in;

import com.devrenno.bookland.auth.application.dto.TokenResponse;

public interface RefreshAccessTokenUseCase {
    TokenResponse execute(String refreshTokenValue);
}
