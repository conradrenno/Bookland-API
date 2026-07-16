package com.devrenno.bookland.user.application.port.in;

import com.devrenno.bookland.user.domain.entity.User;
import com.devrenno.bookland.user.domain.valueobject.UserId;

public interface GetUserByIdUseCase {
    User execute(UserId id);
}
