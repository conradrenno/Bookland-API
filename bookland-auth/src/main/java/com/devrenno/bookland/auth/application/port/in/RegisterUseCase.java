package com.devrenno.bookland.auth.application.port.in;

import com.devrenno.bookland.auth.application.dto.AuthUserDto;
import com.devrenno.bookland.auth.application.dto.RegisterCommand;

public interface RegisterUseCase {
    AuthUserDto execute(RegisterCommand command);
}
