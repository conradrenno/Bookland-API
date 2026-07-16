package com.devrenno.bookland.auth.application.service;

import com.devrenno.bookland.auth.application.port.in.LogoutUseCase;
import com.devrenno.bookland.auth.application.port.out.RefreshTokenPersistencePort;

public class LogoutService implements LogoutUseCase {

    private final RefreshTokenPersistencePort refreshTokenPersistencePort;

    private LogoutService(RefreshTokenPersistencePort refreshTokenPersistencePort) {
        this.refreshTokenPersistencePort = refreshTokenPersistencePort;
    }

    public static LogoutService create(RefreshTokenPersistencePort refreshTokenPersistencePort) {
        return new LogoutService(refreshTokenPersistencePort);
    }

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
