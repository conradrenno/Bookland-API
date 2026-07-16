package com.devrenno.bookland.auth.application.port.in;

import com.devrenno.bookland.auth.application.dto.LoginCommand;
import com.devrenno.bookland.auth.domain.valueobject.AuthTokens;

public interface LoginUseCase {
    AuthTokens execute(LoginCommand command);
}
