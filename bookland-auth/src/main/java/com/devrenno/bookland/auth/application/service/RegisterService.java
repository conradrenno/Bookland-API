package com.devrenno.bookland.auth.application.service;

import com.devrenno.bookland.auth.application.annotation.UseCase;
import com.devrenno.bookland.auth.application.dto.AuthUserDto;
import com.devrenno.bookland.auth.application.dto.RegisterCommand;
import com.devrenno.bookland.auth.application.dto.TokenResponse;
import com.devrenno.bookland.auth.application.port.in.RegisterUseCase;
import com.devrenno.bookland.auth.application.port.out.RefreshTokenPersistencePort;
import com.devrenno.bookland.auth.application.port.out.TokenProviderPort;
import com.devrenno.bookland.auth.application.port.out.UserRegistrationPort;
import com.devrenno.bookland.auth.domain.entity.RefreshToken;
import com.devrenno.bookland.auth.domain.valueobject.Token;
import com.devrenno.bookland.auth.infrastructure.config.JwtProperties;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class RegisterService implements RegisterUseCase {

    private final UserRegistrationPort userRegistrationPort;
    private final TokenProviderPort tokenProviderPort;
    private final RefreshTokenPersistencePort refreshTokenPersistencePort;
    private final JwtProperties jwtProperties;

    @Override
    public TokenResponse execute(RegisterCommand command) {
        AuthUserDto user = userRegistrationPort.register(
                command.name(), command.email(), command.rawPassword()
        );

        Token accessToken = tokenProviderPort.generate(
                user.id().toString(), user.email(), user.role().name()
        );

        RefreshToken refreshToken = RefreshToken.create(
                user.id(), user.email(), user.role().name(),
                jwtProperties.getRefreshTokenExpirationMs()
        );
        refreshTokenPersistencePort.save(refreshToken);

        return TokenResponse.bearer(
                accessToken.value(), accessToken.expiresAt(),
                refreshToken.getTokenValue(), refreshToken.getExpiresAt()
        );
    }
}
