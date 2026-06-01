package com.devrenno.bookland.auth.application.service;

import com.devrenno.bookland.auth.application.annotation.UseCase;
import com.devrenno.bookland.auth.application.port.in.LogoutUseCase;
import com.devrenno.bookland.auth.application.port.out.RefreshTokenPersistencePort;
import com.devrenno.bookland.auth.domain.entity.RefreshToken;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class LogoutService implements LogoutUseCase {

    private final RefreshTokenPersistencePort refreshTokenPersistencePort;

    @Override
    public void execute(String refreshTokenValue) {
        refreshTokenPersistencePort.findByTokenValue(refreshTokenValue).ifPresent(token -> {
            if (!token.isRevoked()) {
                token.revoke();
                refreshTokenPersistencePort.save(token);
            }
        });
    }
}
