package com.devrenno.bookland.auth.application.controller;

import com.devrenno.bookland.auth.application.annotation.UseCase;
import com.devrenno.bookland.auth.application.dto.AuthUserDto;
import com.devrenno.bookland.auth.application.dto.LoginCommand;
import com.devrenno.bookland.auth.application.dto.TokenResponse;
import com.devrenno.bookland.auth.application.port.in.LoginUseCase;
import com.devrenno.bookland.auth.application.port.out.TokenProviderPort;
import com.devrenno.bookland.auth.application.port.out.UserLookupPort;
import com.devrenno.bookland.auth.domain.exception.InvalidCredentialsException;
import com.devrenno.bookland.auth.domain.valueobject.Token;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

@UseCase
@RequiredArgsConstructor
public class AuthApplicationController implements LoginUseCase {

    private final UserLookupPort userLookupPort;
    private final TokenProviderPort tokenProviderPort;
    private final PasswordEncoder passwordEncoder;

    @Override
    public TokenResponse execute(LoginCommand command) {
        AuthUserDto user = userLookupPort.findByEmail(command.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(command.rawPassword(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        Token token = tokenProviderPort.generate(
                user.id().toString(),
                user.email(),
                user.role()
        );
        return TokenResponse.bearer(token.value(), token.expiresAt());
    }
}
