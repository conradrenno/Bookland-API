package com.devrenno.bookland.auth.api.controller;

import com.devrenno.bookland.auth.api.dto.LoginRequest;
import com.devrenno.bookland.auth.api.dto.TokenApiResponse;
import com.devrenno.bookland.auth.api.mapper.AuthApiMapper;
import com.devrenno.bookland.auth.application.port.in.LoginUseCase;
import com.devrenno.bookland.auth.domain.exception.InvalidCredentialsException;
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
    private final AuthApiMapper mapper;

    @PostMapping("/login")
    public ResponseEntity<TokenApiResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenApiResponse response = mapper.toApiResponse(
                loginUseCase.execute(mapper.toCommand(request))
        );
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }
}
