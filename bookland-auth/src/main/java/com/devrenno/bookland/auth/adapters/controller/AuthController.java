package com.devrenno.bookland.auth.adapters.controller;

import com.devrenno.bookland.auth.adapters.presenter.AuthPresenter;
import com.devrenno.bookland.auth.adapters.viewmodel.RegisteredUserViewModel;
import com.devrenno.bookland.auth.application.dto.RegisterCommand;
import com.devrenno.bookland.auth.application.port.in.RegisterUseCase;
import com.devrenno.bookland.auth.application.port.out.UserRegistrationPort;
import com.devrenno.bookland.auth.application.service.RegisterService;

/**
 * Internal controller: orchestrates the auth use cases and delegates to the Presenter. Also the
 * module's composition root — its create(...) factory wires the use cases from the outbound ports.
 * Framework-free.
 *
 * <p>One method left, from four. Login, refresh and logout are now the Authorization Server's
 * {@code /oauth2/authorize}, {@code /oauth2/token} and {@code /connect/logout} — protocol endpoints
 * the framework serves, not code we maintain. Registration stays because it is the one operation
 * here that is business logic: e-mail uniqueness, the default CUSTOMER role and the invariants of
 * {@code User.create} have nowhere to live inside an OAuth2 endpoint.
 */
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final AuthPresenter presenter;

    private AuthController(RegisterUseCase registerUseCase, AuthPresenter presenter) {
        this.registerUseCase = registerUseCase;
        this.presenter = presenter;
    }

    public static AuthController create(UserRegistrationPort userRegistrationPort) {
        return new AuthController(
                RegisterService.create(userRegistrationPort),
                AuthPresenter.create()
        );
    }

    public RegisteredUserViewModel register(RegisterCommand command) {
        return presenter.present(registerUseCase.execute(command));
    }
}
