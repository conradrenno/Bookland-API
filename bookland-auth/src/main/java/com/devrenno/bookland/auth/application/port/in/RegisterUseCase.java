package com.devrenno.bookland.auth.application.port.in;

import com.devrenno.bookland.auth.application.dto.RegisterCommand;
import com.devrenno.bookland.auth.application.dto.TokenResponse;

public interface RegisterUseCase {
    TokenResponse execute(RegisterCommand command);
}
