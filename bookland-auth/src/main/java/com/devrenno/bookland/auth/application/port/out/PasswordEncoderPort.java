package com.devrenno.bookland.auth.application.port.out;

public interface PasswordEncoderPort {
    boolean matches(String rawPassword, String encodedPassword);
}
