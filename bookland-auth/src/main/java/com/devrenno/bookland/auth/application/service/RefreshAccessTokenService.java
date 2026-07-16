package com.devrenno.bookland.auth.application.service;

import com.devrenno.bookland.auth.application.port.in.RefreshAccessTokenUseCase;
import com.devrenno.bookland.auth.application.port.out.RefreshTokenPersistencePort;
import com.devrenno.bookland.auth.application.port.out.TokenProviderPort;
import com.devrenno.bookland.auth.domain.entity.RefreshToken;
import com.devrenno.bookland.auth.domain.exception.InvalidRefreshTokenException;
import com.devrenno.bookland.auth.domain.valueobject.AuthTokens;
import com.devrenno.bookland.auth.domain.valueobject.Token;

public class RefreshAccessTokenService implements RefreshAccessTokenUseCase {

    private final RefreshTokenPersistencePort refreshTokenPersistencePort;
    private final TokenProviderPort tokenProviderPort;
    private final long refreshTokenExpirationMs;

    private RefreshAccessTokenService(RefreshTokenPersistencePort refreshTokenPersistencePort,
                                      TokenProviderPort tokenProviderPort,
                                      long refreshTokenExpirationMs) {
        this.refreshTokenPersistencePort = refreshTokenPersistencePort;
        this.tokenProviderPort = tokenProviderPort;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public static RefreshAccessTokenService create(RefreshTokenPersistencePort refreshTokenPersistencePort,
                                                   TokenProviderPort tokenProviderPort,
                                                   long refreshTokenExpirationMs) {
        return new RefreshAccessTokenService(refreshTokenPersistencePort, tokenProviderPort, refreshTokenExpirationMs);
    }

    @Override
    public AuthTokens execute(String refreshTokenValue) {
        RefreshToken existing = refreshTokenPersistencePort.findByTokenValue(refreshTokenValue)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (!existing.isValid()) {
            throw new InvalidRefreshTokenException();
        }

        existing.revoke();
        refreshTokenPersistencePort.save(existing);

        Token newAccessToken = tokenProviderPort.generate(
                existing.getUserId().toString(), existing.getEmail(), existing.getRole()
        );

        RefreshToken newRefreshToken = RefreshToken.create(
                existing.getUserId(), existing.getEmail(), existing.getRole(), refreshTokenExpirationMs
        );
        refreshTokenPersistencePort.save(newRefreshToken);

        return new AuthTokens(newAccessToken, newRefreshToken);
    }
}
