package com.devrenno.bookland.user.application.port.in;

import com.devrenno.bookland.user.application.dto.CreateUserCommand;
import com.devrenno.bookland.user.application.dto.UserResponse;

public interface RegisterUserUseCase {
    UserResponse execute(CreateUserCommand command);
}
