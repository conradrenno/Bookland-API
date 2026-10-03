package com.devrenno.bookland.user.application.port.in;

import com.devrenno.bookland.user.domain.entity.User;
import com.devrenno.bookland.user.domain.valueobject.UserId;

/**
 * Active accounts only: a deleted (deactivated) account is {@code UserNotFoundException}, exactly
 * as it was when deleting removed the row. The auth module relies on this to refuse a refresh.
 */
public interface GetUserByIdUseCase {
    User execute(UserId id);
}
