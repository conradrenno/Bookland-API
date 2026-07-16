package com.devrenno.bookland.user.application.port.in;

import com.devrenno.bookland.user.domain.entity.User;
import com.devrenno.bookland.user.domain.valueobject.Email;

public interface GetUserByEmailUseCase {
    User execute(Email email);
}
