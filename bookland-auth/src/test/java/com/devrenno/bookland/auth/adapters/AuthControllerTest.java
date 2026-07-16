package com.devrenno.bookland.auth.adapters;

import com.devrenno.bookland.auth.adapters.controller.AuthController;
import com.devrenno.bookland.auth.adapters.viewmodel.TokenViewModel;
import com.devrenno.bookland.auth.application.dto.AuthUserDto;
import com.devrenno.bookland.auth.application.dto.LoginCommand;
import com.devrenno.bookland.auth.application.port.out.PasswordEncoderPort;
import com.devrenno.bookland.auth.application.port.out.RefreshTokenPersistencePort;
import com.devrenno.bookland.auth.application.port.out.TokenProviderPort;
import com.devrenno.bookland.auth.application.port.out.UserLookupPort;
import com.devrenno.bookland.auth.application.port.out.UserRegistrationPort;
import com.devrenno.bookland.auth.domain.valueobject.Token;
import com.devrenno.bookland.user.domain.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private UserLookupPort userLookupPort;
    @Mock private UserRegistrationPort userRegistrationPort;
    @Mock private TokenProviderPort tokenProviderPort;
    @Mock private RefreshTokenPersistencePort refreshTokenPersistencePort;
    @Mock private PasswordEncoderPort passwordEncoderPort;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = AuthController.create(userLookupPort, userRegistrationPort, tokenProviderPort,
                refreshTokenPersistencePort, passwordEncoderPort, 604800000L);
        when(refreshTokenPersistencePort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void login_shouldReturnBearerTokenViewModel_whenCredentialsValid() {
        UUID userId = UUID.randomUUID();
        AuthUserDto user = new AuthUserDto(userId, "alice@test.com", "hashed", UserRole.CUSTOMER);
        Token token = new Token("jwt-value", Instant.now().plusSeconds(3600), userId, "alice@test.com", "CUSTOMER");

        when(userLookupPort.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoderPort.matches("secret", "hashed")).thenReturn(true);
        when(tokenProviderPort.generate(any(), any(), any())).thenReturn(token);

        TokenViewModel result = controller.login(new LoginCommand("alice@test.com", "secret"));

        assertThat(result.accessToken()).isEqualTo("jwt-value");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.refreshTokenExpiresAt()).isAfter(Instant.now());
    }
}
