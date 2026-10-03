package com.devrenno.bookland.user.application.port.in;

import com.devrenno.bookland.user.domain.entity.User;
import com.devrenno.bookland.user.domain.valueobject.Email;

/**
 * Finds deactivated accounts too, unlike {@link GetUserByIdUseCase}: the login has to see the
 * account to refuse it as disabled, and {@code AdminBootstrap} has to see it to not recreate it.
 */
public interface GetUserByEmailUseCase {
    User execute(Email email);
}
