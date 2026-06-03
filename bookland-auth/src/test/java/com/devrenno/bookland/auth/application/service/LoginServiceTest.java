package com.devrenno.bookland.auth.application.service;

import com.devrenno.bookland.auth.application.dto.AuthUserDto;
import com.devrenno.bookland.auth.application.dto.LoginCommand;
import com.devrenno.bookland.user.domain.entity.UserRole;
import com.devrenno.bookland.auth.application.dto.TokenResponse;
import com.devrenno.bookland.auth.application.port.out.RefreshTokenPersistencePort;
import com.devrenno.bookland.auth.application.port.out.TokenProviderPort;
import com.devrenno.bookland.auth.application.port.out.UserLookupPort;
import com.devrenno.bookland.auth.domain.entity.RefreshToken;
import com.devrenno.bookland.auth.domain.exception.InvalidCredentialsException;
import com.devrenno.bookland.auth.domain.valueobject.Token;
import com.devrenno.bookland.auth.infrastructure.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock private UserLookupPort userLookupPort;
    @Mock private TokenProviderPort tokenProviderPort;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenPersistencePort refreshTokenPersistencePort;
    @Mock private JwtProperties jwtProperties;
    @InjectMocks private LoginService loginService;

    @BeforeEach
    void setUp() {
        lenient().when(jwtProperties.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        lenient().when(refreshTokenPersistencePort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void execute_shouldReturnTokenPair_whenCredentialsAreValid() {
        UUID userId = UUID.randomUUID();
        AuthUserDto user = new AuthUserDto(userId, "alice@test.com", "hashed", UserRole.CUSTOMER);
        Token token = new Token("jwt-value", Instant.now().plusSeconds(3600), userId, "alice@test.com", "CUSTOMER");

        when(userLookupPort.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);
        when(tokenProviderPort.generate(any(), any(), any())).thenReturn(token);

        TokenResponse result = loginService.execute(new LoginCommand("alice@test.com", "secret"));

        assertThat(result.accessToken()).isEqualTo("jwt-value");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.refreshTokenExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void execute_shouldThrow_whenUserNotFound() {
        when(userLookupPort.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginService.execute(new LoginCommand("unknown@test.com", "pass")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void execute_shouldThrow_whenPasswordDoesNotMatch() {
        UUID userId = UUID.randomUUID();
        AuthUserDto user = new AuthUserDto(userId, "alice@test.com", "hashed", UserRole.CUSTOMER);

        when(userLookupPort.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> loginService.execute(new LoginCommand("alice@test.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
