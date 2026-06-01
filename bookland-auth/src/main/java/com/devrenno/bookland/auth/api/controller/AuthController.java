package com.devrenno.bookland.auth.api.controller;

import com.devrenno.bookland.auth.api.dto.*;
import com.devrenno.bookland.auth.api.mapper.AuthApiMapper;
import com.devrenno.bookland.auth.application.port.in.LoginUseCase;
import com.devrenno.bookland.auth.application.port.in.LogoutUseCase;
import com.devrenno.bookland.auth.application.port.in.RefreshAccessTokenUseCase;
import com.devrenno.bookland.auth.application.port.in.RegisterUseCase;
import com.devrenno.bookland.auth.domain.exception.InvalidCredentialsException;
import com.devrenno.bookland.auth.domain.exception.InvalidRefreshTokenException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RegisterUseCase registerUseCase;
    private final RefreshAccessTokenUseCase refreshAccessTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final AuthApiMapper mapper;

    @PostMapping("/register")
    public ResponseEntity<TokenApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        TokenApiResponse response = mapper.toApiResponse(
                registerUseCase.execute(mapper.toRegisterCommand(request))
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenApiResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenApiResponse response = mapper.toApiResponse(
                loginUseCase.execute(mapper.toCommand(request))
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenApiResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenApiResponse response = mapper.toApiResponse(
                refreshAccessTokenUseCase.execute(request.refreshToken())
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        logoutUseCase.execute(request.refreshToken());
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
