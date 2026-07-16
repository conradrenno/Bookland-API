package com.devrenno.bookland.auth.application.service;

import com.devrenno.bookland.auth.application.dto.AuthUserDto;
import com.devrenno.bookland.auth.application.dto.LoginCommand;
import com.devrenno.bookland.auth.application.port.in.LoginUseCase;
import com.devrenno.bookland.auth.application.port.out.PasswordEncoderPort;
import com.devrenno.bookland.auth.application.port.out.RefreshTokenPersistencePort;
import com.devrenno.bookland.auth.application.port.out.TokenProviderPort;
import com.devrenno.bookland.auth.application.port.out.UserLookupPort;
import com.devrenno.bookland.auth.domain.entity.RefreshToken;
import com.devrenno.bookland.auth.domain.exception.InvalidCredentialsException;
import com.devrenno.bookland.auth.domain.valueobject.AuthTokens;
import com.devrenno.bookland.auth.domain.valueobject.Token;

public class LoginService implements LoginUseCase {

    private final UserLookupPort userLookupPort;
    private final TokenProviderPort tokenProviderPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final RefreshTokenPersistencePort refreshTokenPersistencePort;
    private final long refreshTokenExpirationMs;

    private LoginService(UserLookupPort userLookupPort,
                         TokenProviderPort tokenProviderPort,
                         PasswordEncoderPort passwordEncoderPort,
                         RefreshTokenPersistencePort refreshTokenPersistencePort,
                         long refreshTokenExpirationMs) {
        this.userLookupPort = userLookupPort;
        this.tokenProviderPort = tokenProviderPort;
        this.passwordEncoderPort = passwordEncoderPort;
        this.refreshTokenPersistencePort = refreshTokenPersistencePort;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public static LoginService create(UserLookupPort userLookupPort,
                                      TokenProviderPort tokenProviderPort,
                                      PasswordEncoderPort passwordEncoderPort,
                                      RefreshTokenPersistencePort refreshTokenPersistencePort,
                                      long refreshTokenExpirationMs) {
        return new LoginService(userLookupPort, tokenProviderPort, passwordEncoderPort,
                refreshTokenPersistencePort, refreshTokenExpirationMs);
    }

    @Override
    public AuthTokens execute(LoginCommand command) {
        AuthUserDto user = userLookupPort.findByEmail(command.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoderPort.matches(command.rawPassword(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        Token accessToken = tokenProviderPort.generate(
                user.id().toString(),
                user.email(),
                user.role().name()
        );

        RefreshToken refreshToken = RefreshToken.create(
                user.id(), user.email(), user.role().name(), refreshTokenExpirationMs
        );
        refreshTokenPersistencePort.save(refreshToken);

        return new AuthTokens(accessToken, refreshToken);
    }
}
