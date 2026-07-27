package com.devrenno.bookland.auth.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        // The upper bound is BCrypt's: it silently ignores anything past 72 bytes, so accepting
        // more would mean accepting a password that is not fully checked at login. Without it the
        // generated message also reads "size must be between 8 and 2147483647".
        @NotBlank
        @Size(min = 8, max = 72)
        @Pattern(regexp = ".*\\d.*", message = "must contain at least one number")
        String password
) {}
