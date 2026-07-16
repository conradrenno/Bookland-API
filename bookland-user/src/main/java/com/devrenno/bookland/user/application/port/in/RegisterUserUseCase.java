package com.devrenno.bookland.user.application.port.in;

import com.devrenno.bookland.user.application.dto.CreateUserCommand;
import com.devrenno.bookland.user.domain.entity.User;

public interface RegisterUserUseCase {
    User execute(CreateUserCommand command);
}
