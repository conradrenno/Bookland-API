package com.devrenno.bookland.auth.application;

import com.devrenno.bookland.auth.application.controller.AuthApplicationController;
import com.devrenno.bookland.auth.application.dto.AuthUserDto;
import com.devrenno.bookland.auth.application.dto.LoginCommand;
import com.devrenno.bookland.auth.application.dto.TokenResponse;
import com.devrenno.bookland.auth.application.port.out.TokenProviderPort;
import com.devrenno.bookland.auth.application.port.out.UserLookupPort;
import com.devrenno.bookland.auth.domain.exception.InvalidCredentialsException;
import com.devrenno.bookland.auth.domain.valueobject.Token;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthApplicationControllerTest {

    @Mock private UserLookupPort userLookupPort;
    @Mock private TokenProviderPort tokenProviderPort;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private AuthApplicationController controller;

    @Test
    void login_shouldReturnToken_whenCredentialsAreValid() {
        UUID userId = UUID.randomUUID();
        AuthUserDto user = new AuthUserDto(userId, "alice@test.com", "hashed", "CUSTOMER");
        Token token = new Token("jwt-value", Instant.now().plusSeconds(3600), userId, "alice@test.com", "CUSTOMER");

        when(userLookupPort.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);
        when(tokenProviderPort.generate(any(), any(), any())).thenReturn(token);

        TokenResponse result = controller.execute(new LoginCommand("alice@test.com", "secret"));

        assertThat(result.token()).isEqualTo("jwt-value");
        assertThat(result.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void login_shouldThrow_whenUserNotFound() {
        when(userLookupPort.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.execute(new LoginCommand("unknown@test.com", "pass")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_shouldThrow_whenPasswordDoesNotMatch() {
        UUID userId = UUID.randomUUID();
        AuthUserDto user = new AuthUserDto(userId, "alice@test.com", "hashed", "CUSTOMER");

        when(userLookupPort.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> controller.execute(new LoginCommand("alice@test.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
