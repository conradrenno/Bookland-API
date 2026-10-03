package com.devrenno.bookland.auth.application.port.out;

import com.devrenno.bookland.auth.application.dto.AuthUserDto;

import java.util.Optional;
import java.util.UUID;

public interface UserLookupPort {
    Optional<AuthUserDto> findByEmail(String email);

    /** Active accounts only — a deleted account is empty, which is what refusing a refresh needs. */
    Optional<AuthUserDto> findActiveById(UUID id);
}
