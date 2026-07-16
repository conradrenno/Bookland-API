package com.devrenno.bookland.auth.application.service;

import com.devrenno.bookland.auth.application.port.out.RefreshTokenPersistencePort;
import com.devrenno.bookland.auth.domain.entity.RefreshToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogoutServiceTest {

    @Mock private RefreshTokenPersistencePort refreshTokenPersistencePort;

    private LogoutService logoutService;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        logoutService = LogoutService.create(refreshTokenPersistencePort);
    }

    @Test
    void execute_shouldRevokeToken_whenTokenExists() {
        RefreshToken token = RefreshToken.create(userId, "alice@test.com", "CUSTOMER", 604800000L);
        when(refreshTokenPersistencePort.findByTokenValue(token.getTokenValue())).thenReturn(Optional.of(token));
        when(refreshTokenPersistencePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        logoutService.execute(token.getTokenValue());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenPersistencePort).save(captor.capture());
        assertThat(captor.getValue().isRevoked()).isTrue();
    }

    @Test
    void execute_shouldDoNothing_whenTokenNotFound() {
        when(refreshTokenPersistencePort.findByTokenValue("missing")).thenReturn(Optional.empty());

        logoutService.execute("missing");

        verify(refreshTokenPersistencePort, never()).save(any());
    }

    @Test
    void execute_shouldDoNothing_whenTokenAlreadyRevoked() {
        RefreshToken token = RefreshToken.create(userId, "alice@test.com", "CUSTOMER", 604800000L);
        token.revoke();
        when(refreshTokenPersistencePort.findByTokenValue(token.getTokenValue())).thenReturn(Optional.of(token));

        logoutService.execute(token.getTokenValue());

        verify(refreshTokenPersistencePort, never()).save(any());
    }
}
