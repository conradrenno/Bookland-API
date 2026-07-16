package com.devrenno.bookland.auth.infrastructure.web;

import com.devrenno.bookland.auth.adapters.controller.AuthController;
import com.devrenno.bookland.auth.adapters.viewmodel.TokenViewModel;
import com.devrenno.bookland.auth.domain.exception.InvalidCredentialsException;
import com.devrenno.bookland.auth.domain.exception.InvalidRefreshTokenException;
import com.devrenno.bookland.auth.infrastructure.web.dto.LoginRequest;
import com.devrenno.bookland.auth.infrastructure.web.dto.LogoutRequest;
import com.devrenno.bookland.auth.infrastructure.web.dto.RefreshTokenRequest;
import com.devrenno.bookland.auth.infrastructure.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * HTTP adapter. Maps HTTP ⇄ internal AuthController (adapters). Holds no orchestration logic.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthController authController;
    private final AuthRequestMapper requestMapper;

    @PostMapping("/register")
    public ResponseEntity<TokenViewModel> register(@Valid @RequestBody RegisterRequest request) {
        TokenViewModel response = authController.register(requestMapper.toRegisterCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenViewModel> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authController.login(requestMapper.toCommand(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenViewModel> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authController.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authController.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ProblemDetail handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }
}
