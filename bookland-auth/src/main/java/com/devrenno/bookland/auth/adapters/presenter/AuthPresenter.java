package com.devrenno.bookland.auth.adapters.presenter;

import com.devrenno.bookland.auth.adapters.viewmodel.RegisteredUserViewModel;
import com.devrenno.bookland.auth.application.dto.AuthUserDto;

/**
 * Turns the registered account into the delivery-facing view model. Plain Java (no framework).
 *
 * <p>Note what it drops: {@link AuthUserDto} carries the password hash, because it is the credential
 * the authentication path compares against. Nothing outside that path has any use for it, and a
 * presenter is the right place for the omission to be visible.
 */
public class AuthPresenter {

    private AuthPresenter() {
    }

    public static AuthPresenter create() {
        return new AuthPresenter();
    }

    public RegisteredUserViewModel present(AuthUserDto user) {
        return new RegisteredUserViewModel(user.id(), user.email(), user.name(), user.role());
    }
}
