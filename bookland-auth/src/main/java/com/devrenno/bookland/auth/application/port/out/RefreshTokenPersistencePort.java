package com.devrenno.bookland.auth.application.port.out;

import com.devrenno.bookland.auth.domain.entity.RefreshToken;

import java.util.Optional;

public interface RefreshTokenPersistencePort {
    RefreshToken save(RefreshToken token);
    Optional<RefreshToken> findByTokenValue(String tokenValue);
}
