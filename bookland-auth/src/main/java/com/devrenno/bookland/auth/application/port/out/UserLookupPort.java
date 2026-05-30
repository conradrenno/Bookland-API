package com.devrenno.bookland.auth.application.port.out;

import com.devrenno.bookland.auth.application.dto.AuthUserDto;

import java.util.Optional;

public interface UserLookupPort {
    Optional<AuthUserDto> findByEmail(String email);
}
