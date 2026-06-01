package com.devrenno.bookland.auth.application.service;

import com.devrenno.bookland.auth.application.annotation.UseCase;
import com.devrenno.bookland.auth.application.dto.TokenResponse;
import com.devrenno.bookland.auth.application.port.in.RefreshAccessTokenUseCase;
import com.devrenno.bookland.auth.application.port.out.RefreshTokenPersistencePort;
import com.devrenno.bookland.auth.application.port.out.TokenProviderPort;
import com.devrenno.bookland.auth.domain.entity.RefreshToken;
import com.devrenno.bookland.auth.domain.exception.InvalidRefreshTokenException;
import com.devrenno.bookland.auth.domain.valueobject.Token;
import com.devrenno.bookland.auth.infrastructure.config.JwtProperties;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class RefreshAccessTokenService implements RefreshAccessTokenUseCase {

    private final RefreshTokenPersistencePort refreshTokenPersistencePort;
    private final TokenProviderPort tokenProviderPort;
    private final JwtProperties jwtProperties;

    @Override
    public TokenResponse execute(String refreshTokenValue) {
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
                existing.getUserId(), existing.getEmail(), existing.getRole(),
                jwtProperties.getRefreshTokenExpirationMs()
        );
        refreshTokenPersistencePort.save(newRefreshToken);

        return TokenResponse.bearer(
                newAccessToken.value(), newAccessToken.expiresAt(),
                newRefreshToken.getTokenValue(), newRefreshToken.getExpiresAt()
        );
    }
}
