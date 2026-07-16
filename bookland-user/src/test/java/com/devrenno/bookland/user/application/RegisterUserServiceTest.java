package com.devrenno.bookland.user.application;

import com.devrenno.bookland.user.application.dto.CreateUserCommand;
import com.devrenno.bookland.user.application.port.out.PasswordEncoderPort;
import com.devrenno.bookland.user.application.port.out.UserPersistencePort;
import com.devrenno.bookland.user.application.service.RegisterUserService;
import com.devrenno.bookland.user.domain.entity.User;
import com.devrenno.bookland.user.domain.entity.UserRole;
import com.devrenno.bookland.user.domain.exception.EmailAlreadyExistsException;
import com.devrenno.bookland.user.domain.service.UserDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserServiceTest {

    @Mock private UserDomainService domainService;
    @Mock private UserPersistencePort persistencePort;
    @Mock private PasswordEncoderPort passwordEncoderPort;

    private RegisterUserService service;

    @BeforeEach
    void setUp() {
        service = RegisterUserService.create(domainService, persistencePort, passwordEncoderPort);
    }

    @Test
    void execute_shouldReturnSavedUser_whenEmailIsNew() {
        CreateUserCommand command = new CreateUserCommand("Alice", "alice@test.com", "secret123", UserRole.CUSTOMER);

        when(persistencePort.existsByEmail(any())).thenReturn(false);
        when(passwordEncoderPort.encode("secret123")).thenReturn("hashed");
        when(persistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        User result = service.execute(command);

        assertThat(result.getEmail().value()).isEqualTo("alice@test.com");
        assertThat(result.getName()).isEqualTo("Alice");
        assertThat(result.getPasswordHash()).isEqualTo("hashed");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void execute_shouldThrow_whenEmailAlreadyExists() {
        CreateUserCommand command = new CreateUserCommand("Alice", "alice@test.com", "secret123", UserRole.CUSTOMER);

        when(persistencePort.existsByEmail(any())).thenReturn(true);
        when(passwordEncoderPort.encode(any())).thenReturn("hashed");
        doThrow(new EmailAlreadyExistsException("alice@test.com"))
                .when(domainService).validateForCreation(any(), any(Boolean.class));

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }
}
