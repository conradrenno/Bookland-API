package com.devrenno.bookland.auth.application.port.in;

import com.devrenno.bookland.auth.application.dto.RegisterCommand;
import com.devrenno.bookland.auth.domain.valueobject.AuthTokens;

public interface RegisterUseCase {
    AuthTokens execute(RegisterCommand command);
}
