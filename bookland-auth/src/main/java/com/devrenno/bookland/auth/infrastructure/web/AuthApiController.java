package com.devrenno.bookland.auth.infrastructure.web;

import com.devrenno.bookland.auth.adapters.controller.AuthController;
import com.devrenno.bookland.auth.adapters.viewmodel.RegisteredUserViewModel;
import com.devrenno.bookland.auth.infrastructure.security.RegistrationSessionEstablisher;
import com.devrenno.bookland.auth.infrastructure.web.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP adapter. Maps HTTP ⇄ internal AuthController (adapters). Holds no orchestration logic.
 *
 * <p>Down to one route. {@code /login}, {@code /refresh} and {@code /logout} are gone: they were a
 * hand-written implementation of what {@code /oauth2/authorize}, {@code /oauth2/token} and
 * {@code /connect/logout} now do, and keeping both would mean two issuers, two notions of session
 * end, and two places to get revocation wrong.
 *
 * <p>Registration is not authentication, which is why it survives here untouched by any of that: no
 * authentication component of Spring Security takes part in it — there is no user yet to
 * authenticate. The only Spring Security class involved is the password encoder, which is a hashing
 * utility rather than an authentication mechanism.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthController authController;
    private final AuthRequestMapper requestMapper;
    private final RegistrationSessionEstablisher sessionEstablisher;

    /**
     * Creates the account and signs the caller in, then reports the account.
     *
     * <p>No token comes back. The client's next step is {@code /oauth2/authorize}, which finds the
     * session this established and returns an authorization code without a second password prompt.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<RegisteredUserViewModel> register(@Valid @RequestBody RegisterRequest request,
                                                            HttpServletRequest httpRequest) {
        RegisteredUserViewModel registered =
                authController.register(requestMapper.toRegisterCommand(request));

        sessionEstablisher.establish(httpRequest, registered);

        return ResponseEntity.status(HttpStatus.CREATED).body(registered);
    }
}
