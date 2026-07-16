package com.devrenno.bookland.user.infrastructure.web;

import com.devrenno.bookland.user.adapters.controller.UserController;
import com.devrenno.bookland.user.adapters.viewmodel.UserViewModel;
import com.devrenno.bookland.user.infrastructure.web.dto.UpdateUserRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * HTTP adapter. Maps HTTP ⇄ internal controller (adapters). Holds no orchestration logic.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserApiController {

    private final UserController userController;
    private final UserRequestMapper requestMapper;

    @GetMapping("/{id}")
    public ResponseEntity<UserViewModel> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(userController.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserViewModel> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userController.update(id, requestMapper.toCommand(request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userController.delete(id);
        return ResponseEntity.noContent().build();
    }
}
