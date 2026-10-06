package com.devrenno.bookland.auth.adapters;

import com.devrenno.bookland.auth.adapters.controller.AuthController;
import com.devrenno.bookland.auth.adapters.viewmodel.RegisteredUserViewModel;
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
class AuthControllerTest {

    @Mock
    private UserRegistrationPort userRegistrationPort;

    @Test
    @DisplayName("registering reports the account and hands out no credential of any kind")
    void registerReportsTheAccountWithoutIssuingAToken() {
        UUID userId = UUID.randomUUID();
        when(userRegistrationPort.register("Bob", "bob@test.com", "password1"))
                .thenReturn(new AuthUserDto(userId, "bob@test.com", "Ana Souza", "hashed", UserRole.CUSTOMER, true));

        AuthController controller = AuthController.create(userRegistrationPort);

        RegisteredUserViewModel result =
                controller.register(new RegisterCommand("Bob", "bob@test.com", "password1"));

        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.email()).isEqualTo("bob@test.com");
        assertThat(result.role()).isEqualTo(UserRole.CUSTOMER);
    }

    /**
     * The password hash reaches this layer inside {@code AuthUserDto} — it is the credential the
     * authentication path compares against. Nothing downstream of registering has any use for it,
     * and the view model is what a client actually receives.
     */
    @Test
    @DisplayName("the password hash does not survive into the view model")
    void passwordHashIsNotPresented() {
        when(userRegistrationPort.register("Bob", "bob@test.com", "password1"))
                .thenReturn(new AuthUserDto(UUID.randomUUID(), "bob@test.com", "Ana Souza", "hashed", UserRole.CUSTOMER, true));

        AuthController controller = AuthController.create(userRegistrationPort);

        RegisteredUserViewModel result =
                controller.register(new RegisterCommand("Bob", "bob@test.com", "password1"));

        assertThat(result.toString()).doesNotContain("hashed");
    }
}
