package com.devrenno.bookland.user.domain;

import com.devrenno.bookland.user.domain.entity.User;
import com.devrenno.bookland.user.domain.entity.UserRole;
import com.devrenno.bookland.user.domain.exception.EmailAlreadyExistsException;
import com.devrenno.bookland.user.domain.service.UserDomainService;
import com.devrenno.bookland.user.domain.valueobject.Email;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class UserDomainServiceTest {

    private final UserDomainService service = new UserDomainService();

    @Test
    void validateForCreation_shouldThrow_whenEmailAlreadyExists() {
        User user = User.create("John", Email.of("john@example.com"), "hash", UserRole.CUSTOMER);

        assertThatThrownBy(() -> service.validateForCreation(user, true))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("john@example.com");
    }

    @Test
    void validateForCreation_shouldPass_whenEmailIsNew() {
        User user = User.create("John", Email.of("john@example.com"), "hash", UserRole.CUSTOMER);

        assertThatCode(() -> service.validateForCreation(user, false))
                .doesNotThrowAnyException();
    }
}
