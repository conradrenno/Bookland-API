package com.devrenno.bookland.auth.application.service;

import com.devrenno.bookland.auth.application.dto.AuthUserDto;
import com.devrenno.bookland.auth.application.dto.RegisterCommand;
import com.devrenno.bookland.auth.application.port.in.RegisterUseCase;
import com.devrenno.bookland.auth.application.port.out.UserRegistrationPort;

/**
 * Creates the account, and stops there.
 *
 * <p>It used to finish by minting an access token and a refresh token. It cannot any more: issuing a
 * token outside the authorization code flow would be inventing a grant the Authorization Server does
 * not define, and would put a second, hand-rolled issuer next to the one that now owns the job.
 *
 * <p>What replaced that step is not in this class and must not be: establishing the caller's session
 * is an HTTP concern, and this layer is framework-free — it has no session, no request and no
 * response to touch. See the web layer.
 */
public class RegisterService implements RegisterUseCase {

    private final UserRegistrationPort userRegistrationPort;

    private RegisterService(UserRegistrationPort userRegistrationPort) {
        this.userRegistrationPort = userRegistrationPort;
    }

    public static RegisterService create(UserRegistrationPort userRegistrationPort) {
        return new RegisterService(userRegistrationPort);
    }

    @Override
    public AuthUserDto execute(RegisterCommand command) {
        return userRegistrationPort.register(command.name(), command.email(), command.rawPassword());
    }
}
