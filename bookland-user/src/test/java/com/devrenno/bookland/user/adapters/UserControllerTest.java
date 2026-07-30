package com.devrenno.bookland.user.adapters;

import com.devrenno.bookland.user.adapters.controller.UserController;
import com.devrenno.bookland.user.adapters.viewmodel.UserViewModel;
import com.devrenno.bookland.user.application.port.out.UserPersistencePort;
import com.devrenno.bookland.user.domain.entity.User;
import com.devrenno.bookland.user.domain.entity.UserRole;
import com.devrenno.bookland.user.domain.exception.UserNotFoundException;
import com.devrenno.bookland.user.domain.valueobject.Email;
import com.devrenno.bookland.user.domain.valueobject.UserId;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock private UserPersistencePort persistencePort;

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = UserController.create(persistencePort);
    }

    private User sampleUser(UUID id) {
        return User.reconstitute(
                UserId.of(id), "Alice", Email.of("alice@test.com"), "hash",
                UserRole.CUSTOMER, Instant.now(), Instant.now(), true);
    }

    @Test
    void getById_shouldReturnViewModel_withoutPasswordHash() {
        UUID id = UUID.randomUUID();
        when(persistencePort.findById(UserId.of(id))).thenReturn(Optional.of(sampleUser(id)));

        UserViewModel result = controller.getById(id);

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.email()).isEqualTo("alice@test.com");
        assertThat(result.role()).isEqualTo(UserRole.CUSTOMER);
        assertThat(result.active()).isTrue();
    }

    @Test
    void delete_shouldRemoveUser_whenUserExists() {
        UUID id = UUID.randomUUID();
        when(persistencePort.findById(UserId.of(id))).thenReturn(Optional.of(sampleUser(id)));

        controller.delete(id);

        verify(persistencePort).delete(UserId.of(id));
    }

    @Test
    void delete_shouldThrow_whenUserMissing() {
        UUID id = UUID.randomUUID();
        when(persistencePort.findById(UserId.of(id))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.delete(id))
                .isInstanceOf(UserNotFoundException.class);
    }
}
