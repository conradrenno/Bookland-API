package com.devrenno.bookland.auth.application.service;

import com.devrenno.bookland.auth.application.dto.AuthUserDto;
import com.devrenno.bookland.auth.application.dto.RegisterCommand;
import com.devrenno.bookland.user.domain.entity.UserRole;
import com.devrenno.bookland.auth.application.dto.TokenResponse;
import com.devrenno.bookland.auth.application.port.out.RefreshTokenPersistencePort;
import com.devrenno.bookland.auth.application.port.out.TokenProviderPort;
import com.devrenno.bookland.auth.application.port.out.UserRegistrationPort;
import com.devrenno.bookland.auth.domain.valueobject.Token;
import com.devrenno.bookland.auth.infrastructure.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    @Mock private UserRegistrationPort userRegistrationPort;
    @Mock private TokenProviderPort tokenProviderPort;
    @Mock private RefreshTokenPersistencePort refreshTokenPersistencePort;
    @Mock private JwtProperties jwtProperties;
    @InjectMocks private RegisterService registerService;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(jwtProperties.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        when(refreshTokenPersistencePort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void execute_shouldReturnTokenPair_afterSuccessfulRegistration() {
        AuthUserDto user = new AuthUserDto(userId, "bob@test.com", "hashed", UserRole.CUSTOMER);
        Token accessToken = new Token("access-jwt", Instant.now().plusSeconds(3600), userId, "bob@test.com", "CUSTOMER");

        when(userRegistrationPort.register("Bob", "bob@test.com", "password1")).thenReturn(user);
        when(tokenProviderPort.generate(eq(userId.toString()), eq("bob@test.com"), eq("CUSTOMER")))
                .thenReturn(accessToken);

        TokenResponse result = registerService.execute(new RegisterCommand("Bob", "bob@test.com", "password1"));

        assertThat(result.accessToken()).isEqualTo("access-jwt");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.refreshTokenExpiresAt()).isAfter(Instant.now());
        verify(refreshTokenPersistencePort).save(any());
    }
}
