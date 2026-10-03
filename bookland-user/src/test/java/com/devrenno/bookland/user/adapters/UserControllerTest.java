package com.devrenno.bookland.user.adapters;

import com.devrenno.bookland.user.adapters.controller.UserController;
import com.devrenno.bookland.user.adapters.viewmodel.UserViewModel;
import com.devrenno.bookland.user.application.dto.UpdateUserCommand;
import com.devrenno.bookland.user.application.port.out.UserPersistencePort;
import com.devrenno.bookland.user.domain.entity.User;
import com.devrenno.bookland.user.domain.entity.UserRole;
import com.devrenno.bookland.user.domain.exception.AdminAccountDeactivationException;
import com.devrenno.bookland.user.domain.exception.UserAccessDeniedException;
import com.devrenno.bookland.user.domain.exception.UserNotFoundException;
import com.devrenno.bookland.user.domain.valueobject.Email;
import com.devrenno.bookland.user.domain.valueobject.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

        UserViewModel result = controller.getById(id, id);

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.email()).isEqualTo("alice@test.com");
        assertThat(result.role()).isEqualTo(UserRole.CUSTOMER);
        assertThat(result.active()).isTrue();
    }

    @Test
    void delete_shouldDeactivateInsteadOfRemoving() {
        UUID id = UUID.randomUUID();
        when(persistencePort.findById(UserId.of(id))).thenReturn(Optional.of(sampleUser(id)));

        controller.delete(id, id);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(persistencePort).save(saved.capture());
        assertThat(saved.getValue().getId().value()).isEqualTo(id);
        assertThat(saved.getValue().isActive()).isFalse();
    }

    @Test
    void delete_shouldRefuseAnAdminAccount() {
        UUID id = UUID.randomUUID();
        User admin = User.reconstitute(
                UserId.of(id), "Admin", Email.of("admin@test.com"), "hash",
                UserRole.ADMIN, Instant.now(), Instant.now(), true);
        when(persistencePort.findById(UserId.of(id))).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> controller.delete(id, id))
                .isInstanceOf(AdminAccountDeactivationException.class);
        verify(persistencePort, never()).save(any());
    }

    @Test
    void deactivatedAccount_shouldBeNotFound_toEveryIdLookup() {
        UUID id = UUID.randomUUID();
        User user = sampleUser(id);
        user.deactivate();
        when(persistencePort.findById(UserId.of(id))).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> controller.getById(id, id)).isInstanceOf(UserNotFoundException.class);
        assertThatThrownBy(() -> controller.update(id, id, new UpdateUserCommand("Bob")))
                .isInstanceOf(UserNotFoundException.class);
        assertThatThrownBy(() -> controller.delete(id, id)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void delete_shouldThrow_whenUserMissing() {
        UUID id = UUID.randomUUID();
        when(persistencePort.findById(UserId.of(id))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.delete(id, id))
                .isInstanceOf(UserNotFoundException.class);
    }

    /**
     * These routes address an account by id, so without the caller they cannot tell "my account"
     * from "anyone's". They were reachable by any authenticated user, which made reading, rewriting
     * and deleting somebody else's account — the admin's included — a matter of changing one UUID.
     */
    @Nested
    @DisplayName("an account belongs to exactly one caller")
    class Ownership {

        private final UUID target = UUID.randomUUID();
        private final UUID intruder = UUID.randomUUID();

        @Test
        @DisplayName("reading someone else's account is denied")
        void getByIdDeniesAnotherAccount() {
            assertThatThrownBy(() -> controller.getById(target, intruder))
                    .isInstanceOf(UserAccessDeniedException.class);
        }

        @Test
        @DisplayName("updating someone else's account is denied")
        void updateDeniesAnotherAccount() {
            assertThatThrownBy(() -> controller.update(target, intruder, new UpdateUserCommand("Mallory")))
                    .isInstanceOf(UserAccessDeniedException.class);
        }

        @Test
        @DisplayName("deleting someone else's account is denied")
        void deleteDeniesAnotherAccount() {
            assertThatThrownBy(() -> controller.delete(target, intruder))
                    .isInstanceOf(UserAccessDeniedException.class);
        }

        /**
         * The denial lands before the lookup, so the answer is the same whether or not the account
         * exists. Otherwise a 403-versus-404 difference turns the route into an oracle for which
         * user ids are real.
         */
        @Test
        @DisplayName("the denial does not first reveal whether the account exists")
        void deniesWithoutTouchingPersistence() {
            assertThatThrownBy(() -> controller.getById(target, intruder))
                    .isInstanceOf(UserAccessDeniedException.class);

            verifyNoInteractions(persistencePort);
        }
    }
}
