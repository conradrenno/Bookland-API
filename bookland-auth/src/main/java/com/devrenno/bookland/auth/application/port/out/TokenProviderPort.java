package com.devrenno.bookland.auth.application.port.out;

import com.devrenno.bookland.auth.domain.valueobject.Token;

public interface TokenProviderPort {
    Token generate(String userId, String email, String role);
    Token validate(String tokenValue);
}
