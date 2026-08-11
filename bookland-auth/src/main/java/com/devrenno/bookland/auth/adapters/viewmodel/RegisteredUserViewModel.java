package com.devrenno.bookland.auth.adapters.viewmodel;

import com.devrenno.bookland.user.domain.entity.UserRole;

import java.util.UUID;

/**
 * What registering answers now that it no longer answers with tokens.
 *
 * <p>Registration is not authentication — there is nobody to authenticate until it finishes — so it
 * cannot issue tokens without inventing a grant outside the Authorization Server it feeds. What it
 * does instead is establish the server-side session, and report the account it created.
 *
 * <p>Carries no credential of any kind: the caller continues to {@code /oauth2/authorize}, which
 * finds the session and returns an authorization code without asking for the password a second time.
 */
public record RegisteredUserViewModel(
        UUID id,
        String email,
        UserRole role
) {
}
