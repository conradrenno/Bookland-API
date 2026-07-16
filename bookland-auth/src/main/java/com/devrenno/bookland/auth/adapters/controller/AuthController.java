package com.devrenno.bookland.auth.adapters.controller;

import com.devrenno.bookland.auth.adapters.presenter.AuthPresenter;
import com.devrenno.bookland.auth.adapters.viewmodel.TokenViewModel;
import com.devrenno.bookland.auth.application.dto.LoginCommand;
import com.devrenno.bookland.auth.application.dto.RegisterCommand;
import com.devrenno.bookland.auth.application.port.in.LoginUseCase;
import com.devrenno.bookland.auth.application.port.in.LogoutUseCase;
import com.devrenno.bookland.auth.application.port.in.RefreshAccessTokenUseCase;
import com.devrenno.bookland.auth.application.port.in.RegisterUseCase;
import com.devrenno.bookland.auth.application.port.out.PasswordEncoderPort;
import com.devrenno.bookland.auth.application.port.out.RefreshTokenPersistencePort;
import com.devrenno.bookland.auth.application.port.out.TokenProviderPort;
import com.devrenno.bookland.auth.application.port.out.UserLookupPort;
import com.devrenno.bookland.auth.application.port.out.UserRegistrationPort;
import com.devrenno.bookland.auth.application.service.LoginService;
import com.devrenno.bookland.auth.application.service.LogoutService;
import com.devrenno.bookland.auth.application.service.RefreshAccessTokenService;
import com.devrenno.bookland.auth.application.service.RegisterService;

/**
 * Internal controller: orchestrates the auth use cases and delegates to the Presenter.
 * Also the module's composition root — its create(...) factory wires the use cases from the
 * outbound ports (as interfaces) plus the refresh-token TTL provided by infrastructure. Framework-free.
 */
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RegisterUseCase registerUseCase;
    private final RefreshAccessTokenUseCase refreshAccessTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final AuthPresenter presenter;

    private AuthController(LoginUseCase loginUseCase,
                          RegisterUseCase registerUseCase,
                          RefreshAccessTokenUseCase refreshAccessTokenUseCase,
                          LogoutUseCase logoutUseCase,
                          AuthPresenter presenter) {
        this.loginUseCase = loginUseCase;
        this.registerUseCase = registerUseCase;
        this.refreshAccessTokenUseCase = refreshAccessTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.presenter = presenter;
    }

    public static AuthController create(UserLookupPort userLookupPort,
                                       UserRegistrationPort userRegistrationPort,
                                       TokenProviderPort tokenProviderPort,
                                       RefreshTokenPersistencePort refreshTokenPersistencePort,
                                       PasswordEncoderPort passwordEncoderPort,
                                       long refreshTokenExpirationMs) {
        return new AuthController(
                LoginService.create(userLookupPort, tokenProviderPort, passwordEncoderPort,
                        refreshTokenPersistencePort, refreshTokenExpirationMs),
                RegisterService.create(userRegistrationPort, tokenProviderPort,
                        refreshTokenPersistencePort, refreshTokenExpirationMs),
                RefreshAccessTokenService.create(refreshTokenPersistencePort, tokenProviderPort,
                        refreshTokenExpirationMs),
                LogoutService.create(refreshTokenPersistencePort),
                AuthPresenter.create()
        );
    }

    public TokenViewModel login(LoginCommand command) {
        return presenter.present(loginUseCase.execute(command));
    }

    public TokenViewModel register(RegisterCommand command) {
        return presenter.present(registerUseCase.execute(command));
    }

    public TokenViewModel refresh(String refreshTokenValue) {
        return presenter.present(refreshAccessTokenUseCase.execute(refreshTokenValue));
    }

    public void logout(String refreshTokenValue) {
        logoutUseCase.execute(refreshTokenValue);
    }
}
