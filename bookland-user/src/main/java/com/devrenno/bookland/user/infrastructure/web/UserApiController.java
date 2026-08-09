package com.devrenno.bookland.user.infrastructure.web;

import com.devrenno.bookland.user.adapters.controller.UserController;
import com.devrenno.bookland.user.adapters.viewmodel.UserViewModel;
import com.devrenno.bookland.user.infrastructure.web.dto.UpdateUserRequest;
import com.devrenno.bookland.websupport.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * HTTP adapter. Maps HTTP ⇄ internal controller (adapters). Holds no orchestration logic.
 *
 * <p>Every handler here takes the caller as well as the path id: these routes address an account by
 * id, and without the caller the module cannot tell "read my account" from "read anyone's". They
 * are self-service routes — a caller may only reach their own account. Administering other people's
 * accounts, if it is ever needed, belongs on {@code /api/v1/admin/**} with its own controller and
 * its own {@code hasRole("ADMIN")} rule, the way the order back-office already works.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserApiController {

    private final UserController userController;
    private final UserRequestMapper requestMapper;

    @GetMapping("/{id}")
    public ResponseEntity<UserViewModel> getById(@PathVariable UUID id, AuthenticatedUser caller) {
        return ResponseEntity.ok(userController.getById(id, caller.id()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserViewModel> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request,
            AuthenticatedUser caller) {
        return ResponseEntity.ok(userController.update(id, caller.id(), requestMapper.toCommand(request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> delete(@PathVariable UUID id, AuthenticatedUser caller) {
        userController.delete(id, caller.id());
        return ResponseEntity.noContent().build();
    }
}
