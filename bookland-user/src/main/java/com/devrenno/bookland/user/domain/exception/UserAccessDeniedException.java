package com.devrenno.bookland.user.domain.exception;

import java.util.UUID;

/**
 * A caller reached an account that is not theirs. A business 403, not a role problem — see
 * docs/error-contract.md.
 */
public class UserAccessDeniedException extends RuntimeException {

    public UserAccessDeniedException(UUID id) {
        super("Access denied to user: " + id);
    }
}
