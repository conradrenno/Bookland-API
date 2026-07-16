package com.devrenno.bookland.user.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequest(
        @NotBlank String name
) {}
