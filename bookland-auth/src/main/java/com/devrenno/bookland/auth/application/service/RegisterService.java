package com.devrenno.bookland.auth.application.service;

import com.devrenno.bookland.auth.application.dto.AuthUserDto;
import com.devrenno.bookland.auth.application.dto.RegisterCommand;
import com.devrenno.bookland.auth.application.port.in.RegisterUseCase;
import com.devrenno.bookland.auth.application.port.out.RefreshTokenPersistencePort;
import com.devrenno.bookland.auth.application.port.out.TokenProviderPort;
import com.devrenno.bookland.auth.application.port.out.UserRegistrationPort;
import com.devrenno.bookland.auth.domain.entity.RefreshToken;
import com.devrenno.bookland.auth.domain.valueobject.AuthTokens;
import com.devrenno.bookland.auth.domain.valueobject.Token;

public class RegisterService implements RegisterUseCase {

    private final UserRegistrationPort userRegistrationPort;
    private final TokenProviderPort tokenProviderPort;
    private final RefreshTokenPersistencePort refreshTokenPersistencePort;
    private final long refreshTokenExpirationMs;

    private RegisterService(UserRegistrationPort userRegistrationPort,
                            TokenProviderPort tokenProviderPort,
                            RefreshTokenPersistencePort refreshTokenPersistencePort,
                            long refreshTokenExpirationMs) {
        this.userRegistrationPort = userRegistrationPort;
        this.tokenProviderPort = tokenProviderPort;
        this.refreshTokenPersistencePort = refreshTokenPersistencePort;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public static RegisterService create(UserRegistrationPort userRegistrationPort,
                                         TokenProviderPort tokenProviderPort,
                                         RefreshTokenPersistencePort refreshTokenPersistencePort,
                                         long refreshTokenExpirationMs) {
        return new RegisterService(userRegistrationPort, tokenProviderPort,
                refreshTokenPersistencePort, refreshTokenExpirationMs);
    }

    @Override
    public AuthTokens execute(RegisterCommand command) {
        AuthUserDto user = userRegistrationPort.register(
                command.name(), command.email(), command.rawPassword()
        );

        Token accessToken = tokenProviderPort.generate(
                user.id().toString(), user.email(), user.role().name()
        );

        RefreshToken refreshToken = RefreshToken.create(
                user.id(), user.email(), user.role().name(), refreshTokenExpirationMs
        );
        refreshTokenPersistencePort.save(refreshToken);

        return new AuthTokens(accessToken, refreshToken);
    }
}
