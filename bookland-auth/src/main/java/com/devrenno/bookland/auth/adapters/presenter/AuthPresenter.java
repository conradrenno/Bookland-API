package com.devrenno.bookland.auth.adapters.presenter;

import com.devrenno.bookland.auth.adapters.viewmodel.TokenViewModel;
import com.devrenno.bookland.auth.domain.valueobject.AuthTokens;

/**
 * Transforms the domain AuthTokens into the delivery-facing TokenViewModel. Plain Java (no framework).
 */
public class AuthPresenter {

    private static final String TOKEN_TYPE = "Bearer";

    private AuthPresenter() {
    }

    public static AuthPresenter create() {
        return new AuthPresenter();
    }

    public TokenViewModel present(AuthTokens tokens) {
        return new TokenViewModel(
                tokens.accessToken().value(),
                TOKEN_TYPE,
                tokens.accessToken().expiresAt(),
                tokens.refreshToken().getTokenValue(),
                tokens.refreshToken().getExpiresAt()
        );
    }
}
