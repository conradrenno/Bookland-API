package com.devrenno.bookland.auth.application.port.in;

public interface LogoutUseCase {
    void execute(String refreshTokenValue);
}
