package com.devrenno.bookland.auth.application.service;

import com.devrenno.bookland.auth.application.port.out.RefreshTokenPersistencePort;
import com.devrenno.bookland.auth.application.port.out.TokenProviderPort;
import com.devrenno.bookland.auth.domain.entity.RefreshToken;
import com.devrenno.bookland.auth.domain.exception.InvalidRefreshTokenException;
import com.devrenno.bookland.auth.domain.valueobject.AuthTokens;
import com.devrenno.bookland.auth.domain.valueobject.Token;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshAccessTokenServiceTest {

    private static final long REFRESH_TTL_MS = 604800000L;

    @Mock private RefreshTokenPersistencePort refreshTokenPersistencePort;
    @Mock private TokenProviderPort tokenProviderPort;

    private RefreshAccessTokenService service;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = RefreshAccessTokenService.create(refreshTokenPersistencePort, tokenProviderPort, REFRESH_TTL_MS);
        lenient().when(refreshTokenPersistencePort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void execute_shouldReturnNewTokenPair_whenRefreshTokenIsValid() {
        RefreshToken existing = RefreshToken.create(userId, "alice@test.com", "CUSTOMER", REFRESH_TTL_MS);
        Token newAccess = new Token("new-jwt", Instant.now().plusSeconds(3600), userId, "alice@test.com", "CUSTOMER");

        when(refreshTokenPersistencePort.findByTokenValue(existing.getTokenValue())).thenReturn(Optional.of(existing));
        when(tokenProviderPort.generate(any(), any(), any())).thenReturn(newAccess);

        AuthTokens result = service.execute(existing.getTokenValue());

        assertThat(result.accessToken().value()).isEqualTo("new-jwt");
        assertThat(result.refreshToken().getTokenValue()).isNotBlank();
        assertThat(result.refreshToken().getTokenValue()).isNotEqualTo(existing.getTokenValue());
        verify(refreshTokenPersistencePort, times(2)).save(any());
    }

    @Test
    void execute_shouldThrow_whenRefreshTokenNotFound() {
        when(refreshTokenPersistencePort.findByTokenValue("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute("unknown"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void execute_shouldThrow_whenRefreshTokenIsRevoked() {
        RefreshToken revoked = RefreshToken.create(userId, "alice@test.com", "CUSTOMER", REFRESH_TTL_MS);
        revoked.revoke();

        when(refreshTokenPersistencePort.findByTokenValue(revoked.getTokenValue())).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.execute(revoked.getTokenValue()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void execute_shouldThrow_whenRefreshTokenIsExpired() {
        RefreshToken expired = RefreshToken.reconstitute(
                UUID.randomUUID(), "expired-token", userId, "alice@test.com", "CUSTOMER",
                Instant.now().minusSeconds(1), false, Instant.now().minusSeconds(10)
        );

        when(refreshTokenPersistencePort.findByTokenValue("expired-token")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.execute("expired-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }
}
