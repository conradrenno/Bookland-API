package com.devrenno.bookland.user.api.controller;

import com.devrenno.bookland.user.api.dto.request.UpdateUserRequest;
import com.devrenno.bookland.user.api.dto.response.UserApiResponse;
import com.devrenno.bookland.user.api.mapper.UserApiMapper;
import com.devrenno.bookland.user.application.port.in.*;
import com.devrenno.bookland.user.domain.valueobject.UserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final GetUserByIdUseCase getUserByIdUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    private final UserApiMapper mapper;

    @GetMapping("/{id}")
    public ResponseEntity<UserApiResponse> getById(@PathVariable UUID id) {
        UserApiResponse response = mapper.toApiResponse(
                getUserByIdUseCase.execute(UserId.of(id))
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserApiResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        UserApiResponse response = mapper.toApiResponse(
                updateUserUseCase.execute(UserId.of(id), mapper.toCommand(request))
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteUserUseCase.execute(UserId.of(id));
        return ResponseEntity.noContent().build();
    }
}
