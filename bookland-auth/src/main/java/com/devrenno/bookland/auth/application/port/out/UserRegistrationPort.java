package com.devrenno.bookland.auth.application.port.out;

import com.devrenno.bookland.auth.application.dto.AuthUserDto;

public interface UserRegistrationPort {
    AuthUserDto register(String name, String email, String rawPassword);
}
