package com.devrenno.bookland.user.application;

import com.devrenno.bookland.user.application.controller.UserApplicationController;
import com.devrenno.bookland.user.application.dto.CreateUserCommand;
import com.devrenno.bookland.user.application.dto.UserResponse;
import com.devrenno.bookland.user.application.port.out.PasswordEncoderPort;
import com.devrenno.bookland.user.application.port.out.UserPersistencePort;
import com.devrenno.bookland.user.domain.entity.User;
import com.devrenno.bookland.user.domain.entity.UserRole;
import com.devrenno.bookland.user.domain.exception.EmailAlreadyExistsException;
import com.devrenno.bookland.user.domain.service.UserDomainService;
import com.devrenno.bookland.user.domain.valueobject.Email;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserApplicationControllerTest {

    @Mock private UserDomainService domainService;
    @Mock private UserPersistencePort persistencePort;
    @Mock private PasswordEncoderPort passwordEncoderPort;
    @InjectMocks private UserApplicationController controller;

    @Test
    void register_shouldReturnUserResponse_whenEmailIsNew() {
        CreateUserCommand command = new CreateUserCommand("Alice", "alice@test.com", "secret123", UserRole.CUSTOMER);
        User saved = User.create("Alice", Email.of("alice@test.com"), "hashed", UserRole.CUSTOMER);

        when(persistencePort.existsByEmail(any())).thenReturn(false);
        when(passwordEncoderPort.encode("secret123")).thenReturn("hashed");
        when(persistencePort.save(any())).thenReturn(saved);

        UserResponse result = controller.execute(command);

        assertThat(result.email()).isEqualTo("alice@test.com");
        assertThat(result.name()).isEqualTo("Alice");
        assertThat(result.active()).isTrue();
    }

    @Test
    void register_shouldThrow_whenEmailAlreadyExists() {
        CreateUserCommand command = new CreateUserCommand("Alice", "alice@test.com", "secret123", UserRole.CUSTOMER);

        when(persistencePort.existsByEmail(any())).thenReturn(false);
        when(passwordEncoderPort.encode(any())).thenReturn("hashed");
        org.mockito.Mockito.doThrow(new EmailAlreadyExistsException("alice@test.com"))
                .when(domainService).validateForCreation(any(), any(Boolean.class));

        assertThatThrownBy(() -> controller.execute(command))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }
}
