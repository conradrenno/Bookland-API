package com.devrenno.bookland.auth.application.service;

import com.devrenno.bookland.auth.application.dto.AuthUserDto;
import com.devrenno.bookland.auth.application.dto.RegisterCommand;
import com.devrenno.bookland.auth.application.port.out.RefreshTokenPersistencePort;
import com.devrenno.bookland.auth.application.port.out.TokenProviderPort;
import com.devrenno.bookland.auth.application.port.out.UserRegistrationPort;
import com.devrenno.bookland.auth.domain.valueobject.AuthTokens;
import com.devrenno.bookland.auth.domain.valueobject.Token;
import com.devrenno.bookland.user.domain.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private static final long REFRESH_TTL_MS = 604800000L;

    @Mock private UserRegistrationPort userRegistrationPort;
    @Mock private TokenProviderPort tokenProviderPort;
    @Mock private RefreshTokenPersistencePort refreshTokenPersistencePort;

    private RegisterService registerService;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        registerService = RegisterService.create(userRegistrationPort, tokenProviderPort,
                refreshTokenPersistencePort, REFRESH_TTL_MS);
        when(refreshTokenPersistencePort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void execute_shouldReturnTokenPair_afterSuccessfulRegistration() {
        AuthUserDto user = new AuthUserDto(userId, "bob@test.com", "hashed", UserRole.CUSTOMER);
        Token accessToken = new Token("access-jwt", Instant.now().plusSeconds(3600), userId, "bob@test.com", "CUSTOMER");

        when(userRegistrationPort.register("Bob", "bob@test.com", "password1")).thenReturn(user);
        when(tokenProviderPort.generate(eq(userId.toString()), eq("bob@test.com"), eq("CUSTOMER")))
                .thenReturn(accessToken);

        AuthTokens result = registerService.execute(new RegisterCommand("Bob", "bob@test.com", "password1"));

        assertThat(result.accessToken().value()).isEqualTo("access-jwt");
        assertThat(result.refreshToken().getTokenValue()).isNotBlank();
        assertThat(result.refreshToken().getExpiresAt()).isAfter(Instant.now());
        verify(refreshTokenPersistencePort).save(any());
    }
}
