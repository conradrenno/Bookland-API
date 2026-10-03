package com.devrenno.bookland.user.domain.exception;

import java.util.UUID;

public class AdminAccountDeactivationException extends RuntimeException {

    public AdminAccountDeactivationException(UUID id) {
        super("An admin account cannot be deleted: " + id);
    }
}
