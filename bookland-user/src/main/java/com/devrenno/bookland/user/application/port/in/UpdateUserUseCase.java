package com.devrenno.bookland.user.application.port.in;

import com.devrenno.bookland.user.application.dto.UpdateUserCommand;
import com.devrenno.bookland.user.application.dto.UserResponse;
import com.devrenno.bookland.user.domain.valueobject.UserId;

public interface UpdateUserUseCase {
    UserResponse execute(UserId id, UpdateUserCommand command);
}
