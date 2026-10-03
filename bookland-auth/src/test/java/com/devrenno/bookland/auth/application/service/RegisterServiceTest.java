package com.devrenno.bookland.auth.application.service;

import com.devrenno.bookland.auth.application.dto.AuthUserDto;
import com.devrenno.bookland.auth.application.dto.RegisterCommand;
import com.devrenno.bookland.auth.application.port.out.UserRegistrationPort;
import com.devrenno.bookland.user.domain.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    @Mock
    private UserRegistrationPort userRegistrationPort;

    @Test
    @DisplayName("registering delegates the account creation and returns what was created")
    void executeReturnsTheCreatedAccount() {
        UUID userId = UUID.randomUUID();
        AuthUserDto created = new AuthUserDto(userId, "bob@test.com", "hashed", UserRole.CUSTOMER, true);
        when(userRegistrationPort.register("Bob", "bob@test.com", "password1")).thenReturn(created);

        RegisterService service = RegisterService.create(userRegistrationPort);

        AuthUserDto result = service.execute(new RegisterCommand("Bob", "bob@test.com", "password1"));

        assertThat(result).isEqualTo(created);
    }
}
