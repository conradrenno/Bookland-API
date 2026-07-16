package com.devrenno.bookland.user.application.port.in;

import com.devrenno.bookland.user.application.dto.UpdateUserCommand;
import com.devrenno.bookland.user.domain.entity.User;
import com.devrenno.bookland.user.domain.valueobject.UserId;

public interface UpdateUserUseCase {
    User execute(UserId id, UpdateUserCommand command);
}
